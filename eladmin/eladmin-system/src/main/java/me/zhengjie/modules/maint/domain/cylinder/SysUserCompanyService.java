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
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.exception.BusinessException;
import me.zhengjie.modules.maint.domain.cylinder.entity.Company;
import me.zhengjie.modules.maint.domain.cylinder.entity.SysUserCompany;
import me.zhengjie.modules.maint.domain.cylinder.mapper.CompanyMapper;
import me.zhengjie.modules.maint.domain.cylinder.mapper.SysUserCompanyMapper;
import me.zhengjie.modules.maint.util.SecurityContext;
import me.zhengjie.modules.security.service.UserCacheManager;
import me.zhengjie.modules.system.domain.User;
import me.zhengjie.modules.system.mapper.UserMapper;
import me.zhengjie.sys.ResultCodeEnum;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        
        // 4. 【核心防越权：超级管理员拥有上帝视角，直接跳过校验】
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
        
        // 5. 执行绑定（利用 MyBatis-Plus 的 saveOrUpdate 解决主键冲突）
        SysUserCompany binding = new SysUserCompany();
        binding.setUserId(targetUserId);
        binding.setCompanyId(targetCompanyId);
        binding.setCreateBy(SecurityContext.getCurrentUserName());
        
        // 如果该账号之前绑过其他企业，这里会自动覆盖更新；如果没有，则插入新记录
        this.saveOrUpdate(binding);
        
        //清楚user信息缓存
        userCacheManager.cleanUserCache(targetUser.getUsername());
    }
    
}
