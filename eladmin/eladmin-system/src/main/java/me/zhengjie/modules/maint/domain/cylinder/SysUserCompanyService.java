/*
 * Copyright 2026 The cylinder-backend Project under the WTFPL License,
 *
 *     http://www.wtfpl.net/about/
 *
 * Everyone is permitted to copy and distribute verbatim or modified
 * copies of this license document, and changing it is allowed as long
 * as the name is changed.
 *
 * 代码千万行，注释第一行，编程不规范，日后泪两行
 *
 */
package me.zhengjie.modules.maint.domain.cylinder;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.exception.BusinessException;
import me.zhengjie.modules.maint.domain.cylinder.entity.Company;
import me.zhengjie.modules.maint.domain.cylinder.entity.SysUserCompany;
import me.zhengjie.modules.maint.domain.cylinder.mapper.CompanyMapper;
import me.zhengjie.modules.maint.domain.cylinder.mapper.SysUserCompanyMapper;
import me.zhengjie.modules.maint.rest.command.CompanyAccountRegisterCmd;
import me.zhengjie.modules.maint.util.SecurityContext;
import me.zhengjie.modules.security.service.UserCacheManager;
import me.zhengjie.modules.system.domain.Dept;
import me.zhengjie.modules.system.domain.Job;
import me.zhengjie.modules.system.domain.Menu;
import me.zhengjie.modules.system.domain.Role;
import me.zhengjie.modules.system.domain.User;
import me.zhengjie.modules.system.mapper.DeptMapper;
import me.zhengjie.modules.system.mapper.JobMapper;
import me.zhengjie.modules.system.mapper.RoleDeptMapper;
import me.zhengjie.modules.system.mapper.RoleMapper;
import me.zhengjie.modules.system.mapper.RoleMenuMapper;
import me.zhengjie.modules.system.mapper.MenuMapper;
import me.zhengjie.modules.system.mapper.UserJobMapper;
import me.zhengjie.modules.system.mapper.UserMapper;
import me.zhengjie.modules.system.mapper.UserRoleMapper;
import me.zhengjie.sys.ResultCodeEnum;
import me.zhengjie.utils.enums.DataScopeEnum;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户与企业绑定关系
 * @author koma at cylinder-backend
 * @since 2026/3/23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserCompanyService extends ServiceImpl<SysUserCompanyMapper, SysUserCompany> {
    
    private final CompanyMapper companyMapper;
    private final UserMapper sysUserMapper;
    private final UserCacheManager userCacheManager;
    private final DeptMapper deptMapper;
    private final RoleMapper roleMapper;
    private final RoleDeptMapper roleDeptMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final MenuMapper menuMapper;
    private final UserRoleMapper userRoleMapper;
    private final UserJobMapper userJobMapper;
    private final JobMapper jobMapper;
    private final PasswordEncoder passwordEncoder;
    
    
    @Transactional(rollbackFor = Exception.class)
    public void bind(Long targetUserId, Long targetCompanyId) {
        // 1. 获取当前正在操作的管理员的企业 ID 和超级管理员标识
        Boolean isAdmin = SecurityContext.getCurrentUser().getUser().getIsAdmin();
        Long myCompanyId = SecurityContext.getCompanyId();
        
        // 如果既不是超级管理员，又没有绑定过企业，直接阻断
        if (myCompanyId == null && !isAdmin) {
            throw new BusinessException(ResultCodeEnum.COMPANY_NOT_BIND);
        }
        
        // 2. 校验被绑定的用户是否存在
        User targetUser = sysUserMapper.selectById(targetUserId);
        if (targetUser == null) {
            throw new BusinessException(ResultCodeEnum.USER_NOT_EXIST);
        }
        
        // 3. 校验目标企业是否存在
        Company targetCompany = companyMapper.selectById(targetCompanyId);
        if (targetCompany == null) {
            throw new BusinessException(ResultCodeEnum.COMPANY_NOT_EXIST);
        }
        
        // 4. 【新增校验】检查用户是否已被绑定到其他企业（绑定关系一对一）
        SysUserCompany existingBinding = this.baseMapper.selectById(targetUserId);
        if (existingBinding != null && !existingBinding.getCompanyId().equals(targetCompanyId)) {
            // 查询被绑定企业的名称
            Company boundCompany = companyMapper.selectById(existingBinding.getCompanyId());
            String boundCompanyName = boundCompany != null ? boundCompany.getName() : "未知企业";
            throw new BusinessException("该用户已被绑定到企业：" + boundCompanyName + "，绑定关系为一对一，无法重复绑定");
        }
        
        // 5. 【核心防越权：超级管理员拥有上帝视角，直接跳过校验】
        if (!isAdmin) {
            // 场景 A：我在给自己企业的员工建账号并绑定
            boolean isMyOwnCompany = ObjectUtil.equals(targetCompanyId, myCompanyId);
            
            // 场景 B：我是省级代理，我在给市级代理（直接下级）建账号并绑定
            // 💡 使用 ObjectUtil.equals 绝对安全，永远不会报空指针
            boolean isDirectSubordinate = ObjectUtil.equals(myCompanyId, targetCompany.getParentId());
            
            if (!isMyOwnCompany && !isDirectSubordinate) {
                throw new BusinessException(ResultCodeEnum.LEAPFROG_OPERATION);
            }
        }
        
        // 6. 执行绑定（利用 MyBatis-Plus 的 saveOrUpdate 解决主键冲突）
        SysUserCompany binding = new SysUserCompany();
        binding.setUserId(targetUserId);
        binding.setCompanyId(targetCompanyId);
        binding.setCreateBy(SecurityContext.getCurrentUserName());
        
        // 如果该账号之前绑过其他企业，这里会自动覆盖更新；如果没有，则插入新记录
        this.saveOrUpdate(binding);
        
        //清楚user信息缓存
        userCacheManager.cleanUserCache(targetUser.getUsername());
    }

    @Transactional(rollbackFor = Exception.class)
    public Long registerCompanyAccount(CompanyAccountRegisterCmd cmd) {
        Company company = companyMapper.selectById(cmd.getCompanyId());
        if (company == null) {
            throw new BusinessException(ResultCodeEnum.COMPANY_NOT_EXIST);
        }

        checkCanBindCompany(company);
        checkUserUnique(cmd);

        Dept dept = ensureCompanyDept(company);
        Role role = ensureCompanyAdminRole(company, dept);
        Job job = ensureCompanyAdminJob();

        User user = new User();
        user.setUsername(cmd.getUsername());
        user.setNickName(cmd.getNickName());
        user.setPhone(cmd.getPhone());
        user.setEmail(cmd.getEmail());
        user.setGender(cmd.getGender());
        user.setDeptId(dept.getId());
        user.setEnabled(true);
        user.setIsAdmin(false);
        user.setPassword(passwordEncoder.encode("123456"));
        sysUserMapper.insert(user);

        userRoleMapper.insertData(user.getId(), Collections.singleton(role));
        userJobMapper.insertData(user.getId(), Collections.singleton(job));

        SysUserCompany binding = new SysUserCompany();
        binding.setUserId(user.getId());
        binding.setCompanyId(company.getId());
        binding.setCreateBy(SecurityContext.getCurrentUserName());
        this.save(binding);

        return user.getId();
    }

    private void checkCanBindCompany(Company targetCompany) {
        Boolean isAdmin = SecurityContext.getCurrentUser().getUser().getIsAdmin();
        Long myCompanyId = SecurityContext.getCompanyId();
        if (Boolean.TRUE.equals(isAdmin)) {
            return;
        }
        if (myCompanyId == null) {
            throw new BusinessException(ResultCodeEnum.COMPANY_NOT_BIND);
        }
        if (ObjectUtil.equals(myCompanyId, targetCompany.getId())) {
            return;
        }
        if (ObjectUtil.equals(myCompanyId, targetCompany.getParentId())) {
            return;
        }
        throw new BusinessException(ResultCodeEnum.LEAPFROG_OPERATION);
    }

    private void checkUserUnique(CompanyAccountRegisterCmd cmd) {
        if (sysUserMapper.findByUsername(cmd.getUsername()) != null) {
            throw new BusinessException("用户名已存在");
        }
        if (sysUserMapper.findByEmail(cmd.getEmail()) != null) {
            throw new BusinessException("邮箱已存在");
        }
        if (sysUserMapper.findByPhone(cmd.getPhone()) != null) {
            throw new BusinessException("手机号已存在");
        }
    }

    private Dept ensureCompanyDept(Company company) {
        String deptName = buildCompanyDeptName(company);
        Dept existing = deptMapper.selectOne(new LambdaQueryWrapper<Dept>().eq(Dept::getName, deptName));
        if (existing != null) {
            return existing;
        }

        Dept dept = new Dept();
        dept.setName(deptName);
        dept.setEnabled(true);
        dept.setDeptSort(999);
        dept.setPid(resolveParentDeptId(company));
        deptMapper.insert(dept);
        updateSubCount(dept.getPid());
        return dept;
    }

    private Long resolveParentDeptId(Company company) {
        if (company.getParentId() != null && company.getParentId() > 0) {
            Company parent = companyMapper.selectById(company.getParentId());
            if (parent != null) {
                Dept parentDept = deptMapper.selectOne(new LambdaQueryWrapper<Dept>().eq(Dept::getName, buildCompanyDeptName(parent)));
                if (parentDept != null) {
                    return parentDept.getId();
                }
            }
        }
        User currentUser = SecurityContext.getCurrentUser().getUser();
        return currentUser.getDept() == null ? currentUser.getDeptId() : currentUser.getDept().getId();
    }

    private Role ensureCompanyAdminRole(Company company, Dept dept) {
        String roleName = buildCompanyRoleName(company);
        Role existing = roleMapper.findByName(roleName);
        if (existing != null) {
            syncRoleDept(existing.getId(), dept);
            return roleMapper.findById(existing.getId());
        }

        Role role = new Role();
        role.setName(roleName);
        role.setLevel(resolveChildRoleLevel());
        role.setDataScope(DataScopeEnum.CUSTOMIZE.getValue());
        role.setDescription("企业账号自动创建");
        roleMapper.insert(role);
        syncRoleDept(role.getId(), dept);
        syncRoleMenus(role.getId(), company);
        return role;
    }

    private void syncRoleDept(Long roleId, Dept dept) {
        roleDeptMapper.deleteByRoleId(roleId);
        roleDeptMapper.insertData(roleId, Collections.singleton(dept));
    }

    private void syncRoleMenus(Long roleId, Company company) {
        // 先把所有的菜单权限都赋予该角色
        List<Menu> allMenus = menuMapper.selectList(null);
        Set<Menu> menus = allMenus.stream()
                .filter(menu -> menu != null && menu.getId() != null)
                .collect(Collectors.toCollection(HashSet::new));

        roleMenuMapper.deleteByRoleId(roleId);
        if (!menus.isEmpty()) {
            roleMenuMapper.insertData(roleId, menus);
        }

        // 然后删除 menu_id 为 1 的权限
        roleMenuMapper.deleteByMenuId(1L);

        // 删除 pid 为 1 的所有菜单权限（先查询出 pid=1 的菜单 id 列表）
        List<Menu> pidOneMenus = menuMapper.findByPidOrderByMenuSort(1L);
        if (pidOneMenus != null && !pidOneMenus.isEmpty()) {
            for (Menu m : pidOneMenus) {
                if (m != null && m.getId() != null) {
                    roleMenuMapper.deleteByMenuId(m.getId());
                }
            }
        }

        // 如果企业是加气商（typeFiller == 1），再删除 permission 为 "dashboard:map" 的菜单权限
        if (company != null && company.getTypeFiller() != null && company.getTypeFiller() == 1) {
            Menu dashboardMap = menuMapper.selectOne(new LambdaQueryWrapper<Menu>().eq(Menu::getPermission, "dashboard:map"));
            if (dashboardMap != null && dashboardMap.getId() != null) {
                roleMenuMapper.deleteByMenuId(dashboardMap.getId());
            }
        }
    }

    private Set<Menu> resolveCurrentUserMenus() {
        List<Role> roles = roleMapper.findByUserId(SecurityContext.getUserId());
        Set<Menu> menus = roles.stream()
                .filter(role -> role.getMenus() != null)
                .flatMap(role -> role.getMenus().stream())
                .filter(menu -> menu != null && menu.getId() != null)
                .collect(Collectors.toMap(Menu::getId, menu -> menu, (left, right) -> left, LinkedHashMap::new))
                .values()
                .stream()
                .collect(Collectors.toCollection(HashSet::new));
        if (menus.isEmpty()) {
            throw new BusinessException("当前账号没有可继承的菜单权限，无法自动创建企业角色");
        }
        return menus;
    }

    private Integer resolveChildRoleLevel() {
        List<Role> roles = roleMapper.findByUserId(SecurityContext.getUserId());
        Integer currentLevel = roles.stream()
                .map(Role::getLevel)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(2);
        return Math.max(currentLevel + 1, 2);
    }

    private Job ensureCompanyAdminJob() {
        Job existing = jobMapper.findByName("企业管理员");
        if (existing != null) {
            return existing;
        }
        Job job = new Job();
        job.setName("企业管理员");
        job.setEnabled(true);
        job.setJobSort(999L);
        jobMapper.insert(job);
        return job;
    }

    private void updateSubCount(Long deptId) {
        if (deptId != null) {
            deptMapper.updateSubCntById(deptMapper.countByPid(deptId), deptId);
        }
    }

    private String buildCompanyDeptName(Company company) {
        return "企业-" + company.getId() + "-" + company.getName();
    }

    private String buildCompanyRoleName(Company company) {
        return company.getName() + "-企业管理员";
    }
    
}
