/*
 * Copyright 2026 The rfid-backend Project under the WTFPL License,
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

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.exception.BusinessException;
import me.zhengjie.modules.maint.domain.cylinder.entity.*;
import me.zhengjie.modules.maint.domain.cylinder.mapper.*;
import me.zhengjie.modules.maint.domain.dto.CylinderDetailDto;
import me.zhengjie.modules.maint.domain.dto.CylinderFillDto;
import me.zhengjie.modules.maint.domain.dto.CylinderFlowDto;
import me.zhengjie.modules.maint.domain.dto.CylinderPageDto;
import me.zhengjie.modules.maint.domain.enums.*;
import me.zhengjie.modules.maint.rest.command.CylinderQueryReq;
import me.zhengjie.modules.maint.util.RfidParserUtil;
import me.zhengjie.modules.maint.util.SecurityUtils;
import me.zhengjie.modules.system.domain.User;
import me.zhengjie.modules.system.mapper.UserMapper;
import me.zhengjie.utils.PageResult;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 气瓶服务类
 * @author koma at rfid-backend
 * @since 2026/3/17
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CylinderService extends ServiceImpl<CylinderMapper, Cylinder> {
    
    private final ScanRecordMapper scanRecordMapper;
    private final CylinderFlowMapper cylinderFlowMapper;
    private final CompanyMapper companyMapper;
    private final CylinderFillRecordMapper cylinderFillRecordMapper;
    private final CylinderBatchMapper cylinderBatchMapper;
    private final CylinderLifecycleService cylinderLifecycleService;
    private final OperationLogMapper operationLogMapper;
    private final AppUserMapper appUserMapper;
    private final UserMapper sysUserMapper;
    
    
    /**
     * Excel批量导入
     * @param rawDataList
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public int batchImportProduce(List<String> rawDataList) {
        Long myUserId = SecurityUtils.getUserId();
        Long myCompanyId = SecurityUtils.getCompanyId();
        String myCompanyPath = SecurityUtils.getCompanyPath();
        
        // 1. 获取当前企业信息，用于校验监管码
        Company myCompany = companyMapper.selectById(myCompanyId);
        if (myCompany.getTypeManufacturer() != 1) {
            throw new BusinessException(403, "非法操作：只有制造商才能批量导入出厂气瓶！");
        }
        String myMfgCode = myCompany.getCode(); // 【核心】使用企业表中的监管码
        if (StrUtil.isBlank(myMfgCode)) {
            throw new BusinessException(400, "企业信息不完善：未配置监管分配的制造单位代码！");
        }
        
        // ==========================================
        // 2. 内存解析与校验 (O(N) 复杂度)
        // ==========================================
        List<RfidParserUtil.RfidLabel> parsedLabels = new ArrayList<>();
        Set<String> uniqueQrcodes = new HashSet<>(); // 用于当前批次内防重
        
        for (String rawData : rawDataList) {
            // 解析出超级聚合对象
            RfidParserUtil.RfidLabel label = RfidParserUtil.parse(rawData);
            
            // 极限风控 1：比对监管码，防止混入其他厂家的标签
            if (!myMfgCode.equals(label.getUniqueInfo().getMfgCode())) {
                throw new BusinessException(400, "发现异常数据！标签内的制造单位代码 ["
                        + label.getUniqueInfo().getMfgCode() + "] 与本企业不符！");
            }
            
            // 极限风控 2：Excel 内部数据去重防呆
            String code = label.getUniqueInfo().getFullCode();
            if (uniqueQrcodes.contains(code)) {
                continue; // 如果 Excel 里有重复的行，直接跳过
            }
            uniqueQrcodes.add(code);
            parsedLabels.add(label);
        }
        
        // ==========================================
        // 3. 数据库级别防撞码 (极其关键)
        // ==========================================
        // 使用 IN 语句一次性查出数据库中已经存在的二维码
        List<Cylinder> existCylinders = this.baseMapper.selectList(new LambdaQueryWrapper<Cylinder>()
                .in(Cylinder::getCode, uniqueQrcodes)
                .select(Cylinder::getCode)); // 优化性能，只查 code
        
        if (CollUtil.isNotEmpty(existCylinders)) {
            String duplicateCodes = existCylinders.stream().map(Cylinder::getCode).collect(Collectors.joining(","));
            // 实际业务中可能只需要记录错误日志并跳过，这里为了严谨直接抛异常打断
            throw new BusinessException(400, "导入失败！发现系统已存在的重复标签: " + duplicateCodes);
        }
        
        // ==========================================
        // 4. 动态处理生产批次 (CylinderBatch)
        // ==========================================
        // 按照批次号(batchNo)和生产日期进行分组
        Map<String, List<RfidParserUtil.RfidLabel>> batchGroup = parsedLabels.stream()
                                                                             .collect(Collectors.groupingBy(label -> label.getMfgExtInfo().getBatchNo()));
        
        // 建立 批次号 -> 数据库BatchID 的映射字典
        Map<String, Long> batchIdMap = new HashMap<>();
        
        for (Map.Entry<String, List<RfidParserUtil.RfidLabel>> entry : batchGroup.entrySet()) {
            String batchNo = entry.getKey();
            List<RfidParserUtil.RfidLabel> labelsInBatch = entry.getValue();
            // 取该批次第 1 个标签的生产日期作为批次日期
            Date produceDate = labelsInBatch.get(0).getMfgExtInfo().getProduceDate();
            
            // 查一下数据库里有没有建过这个批次
            CylinderBatch existBatch = cylinderBatchMapper.selectOne(new LambdaQueryWrapper<CylinderBatch>()
                    .eq(CylinderBatch::getManufacturerId, myCompanyId)
                    .eq(CylinderBatch::getBatchNo, batchNo));
            
            if (existBatch != null) {
                batchIdMap.put(batchNo, existBatch.getId());
                // 可选：更新该批次的生产总数
                existBatch.setQuantity(existBatch.getQuantity() + labelsInBatch.size());
                cylinderBatchMapper.updateById(existBatch);
            } else {
                // 如果是新批次，自动创建！
                CylinderBatch newBatch = new CylinderBatch();
                newBatch.setManufacturerId(myCompanyId);
                newBatch.setBatchNo(batchNo);
                newBatch.setProduceDate(produceDate);
                newBatch.setQuantity(labelsInBatch.size());
                cylinderBatchMapper.insert(newBatch);
                batchIdMap.put(batchNo, newBatch.getId()); // 拿到刚刚自增的主键 ID
            }
        }
        
        // ==========================================
        // 5. 组装实体，准备高性能批量插入！
        // ==========================================
        Date now = new Date();
        List<Cylinder> insertCylinders = new ArrayList<>();
        List<CylinderLifecycle> insertLifecycles = new ArrayList<>();
        
        for (RfidParserUtil.RfidLabel label : parsedLabels) {
            // 组装气瓶主表
            Cylinder cylinder = new Cylinder();
            cylinder.setType(CylinderType.of(Integer.parseInt(label.getUniqueInfo().getCategoryCode())));
            cylinder.setCode(label.getUniqueInfo().getFullCode());
            cylinder.setSpec(label.getMfgExtInfo().getModelCode());
            cylinder.setVolume(label.getMfgExtInfo().getVolume().doubleValue());
            // 注意：重量(皮重)在 RFID 里没体现，可能需要后续补充，或者让前端多传一个统一个皮重参数
            cylinder.setManufactureDate(label.getMfgExtInfo().getProduceDate());
            cylinder.setNextInspectionDate(label.getMfgExtInfo().getNextInspectDate());
            
            cylinder.setManufacturerId(myCompanyId);
            // 匹配刚才处理好的 BatchID
            cylinder.setBatchId(batchIdMap.get(label.getMfgExtInfo().getBatchNo()));
            cylinder.setCurrentCompanyId(myCompanyId);
            cylinder.setOwnerPath(myCompanyPath);
            cylinder.setCurrentStatus(CylinderStatus.IN_STOCK); // 枚举：1 在库
            
            insertCylinders.add(cylinder);
        }
        
        // MyBatis-Plus 神级方法：分批次插入，每批 1000 条，保护数据库连接
        this.saveBatch(insertCylinders, 1000);
        
        // ==========================================
        // 6. 批量生成生命周期轨迹与操作日志
        // ==========================================
        for (Cylinder savedCylinder : insertCylinders) {
            CylinderLifecycle lifecycle = new CylinderLifecycle();
            lifecycle.setCylinderId(savedCylinder.getId());
            lifecycle.setEventType(LifecycleEventEnum.PRODUCE);
            lifecycle.setCompanyId(myCompanyId);
            lifecycle.setOperatorId(myUserId);
            lifecycle.setAccountType(AccountType.ADMIN);
            lifecycle.setEventTime(now);
            lifecycle.setRemark("Excel 批量扫码建档入库");
            insertLifecycles.add(lifecycle);
        }
        
        // 批量保存生命周期表（如果有对应的 Service，请用 Service 的 saveBatch）
        cylinderLifecycleService.saveBatch(insertLifecycles, 1000);
        
        // 统一记录一次批量操作日志即可，无需每条气瓶记一次日志
        recordOperationLog(OperationType.PRODUCE, TargetType.CYLINDER, (long) parsedLabels.size());
        
        return parsedLabels.size();
    }
    
    
    /**
     * ================= 扫码出库 (发货) =================
     */
    @Transactional(rollbackFor = Exception.class)
    public void scanOut(CylinderFlowDto dto) {
        Long myUserId = SecurityUtils.getUserId();
        Long myCompanyId = SecurityUtils.getCompanyId();
        
        // 1. 查出气瓶
        Cylinder cylinder = this.baseMapper.selectOne(new LambdaQueryWrapper<Cylinder>()
                .eq(Cylinder::getCode, dto.getQrcode()));
        if (cylinder == null) {
            throw new BusinessException(404, "未找到该气瓶信息，请先进行出厂建档激活！");
        }
        
        // 2. 【核心防越权】你只能把你仓库里的气瓶发出去！
        if (!cylinder.getCurrentCompanyId().equals(myCompanyId)) {
            throw new BusinessException(403, "非法操作！该气瓶当前不属于您的网点，无法执行出库！");
        }
        if (ObjectUtil.equals(cylinder.getCurrentStatus(), CylinderStatus.IN_STOCK)) { // 必须是“在库”状态才能出库
            throw new BusinessException(400, "该气瓶当前不是【在库】状态，无法出库！");
        }
        
        // 3. 校验目标企业是否合法
        Company targetCompany = companyMapper.selectById(dto.getTargetCompanyId());
        if (targetCompany == null) {
            throw new BusinessException(400, "指定的接收企业不存在！");
        }
        
        // 4. 记录物理扫码动作
        recordScan(cylinder.getId(), myUserId, myCompanyId, 2); // ScanType 2: 出库扫码
        
        // 5. 记录业务流转轨迹
        Long flowId = recordFlow(cylinder.getId(), myCompanyId, dto.getTargetCompanyId(), 2, myUserId,
                "出库发往: " + targetCompany.getName() + "。备注: " + (dto.getRemark() != null ? dto.getRemark() : ""));
        
        // ==========================================
        // 6. 记录系统级防黑客操作日志
        // ==========================================
        recordOperationLog(OperationType.OUT, TargetType.CYLINDER, cylinder.getId());
        
        // 记录气瓶生命周期
        recordLifecycle(cylinder.getId(), myCompanyId, LifecycleEventEnum.TRANSFER_OUT, "扫码出库", AccountType.APP);
        
        // 7. 更新气瓶主表 (控制权暂时不交接，状态改为运输中)
        Cylinder updateObj = new Cylinder();
        updateObj.setId(cylinder.getId());
        updateObj.setCurrentStatus(CylinderStatus.TRANSIT); // 2: 运输中 / 待接收
        updateObj.setLastFlowId(flowId);
        // 注意：此时 currentCompanyId 依然是发货方，直到接收方扫码入库才正式转移
        this.baseMapper.updateById(updateObj);
    }
    
    /**
     * ================= 扫码入库 (收货) =================
     */
    @Transactional(rollbackFor = Exception.class)
    public void scanIn(CylinderFlowDto dto) {
        Long myUserId = SecurityUtils.getUserId();
        Long myCompanyId = SecurityUtils.getCompanyId();
        String myCompanyPath = SecurityUtils.getCompanyPath();
        
        // 1. 查出气瓶
        Cylinder cylinder = this.baseMapper.selectOne(new LambdaQueryWrapper<Cylinder>()
                .eq(Cylinder::getCode, dto.getQrcode()));
        if (cylinder == null) {
            throw new BusinessException(404, "未找到该气瓶信息！");
        }
        
        // 2. 防呆设计：如果已经在我的库里了，直接提示成功，防止重复扫码报错
        if (cylinder.getCurrentCompanyId().equals(myCompanyId) && ObjectUtil.equals(cylinder.getCurrentStatus(), CylinderStatus.IN_STOCK)) {
            return;
        }
        
        // 3. 记录物理扫码动作
        recordScan(cylinder.getId(), myUserId, myCompanyId, 3); // ScanType 3: 入库扫码
        
        // 4. 记录业务流转轨迹
        Long flowId = recordFlow(cylinder.getId(), cylinder.getCurrentCompanyId(), myCompanyId, 3, myUserId,
                "扫码入库接收完成。备注: " + (dto.getRemark() != null ? dto.getRemark() : ""));
        
        // ==========================================
        // 5. 记录系统级防黑客操作日志
        // ==========================================
        recordOperationLog(OperationType.IN, TargetType.CYLINDER, cylinder.getId());
        
        // 记录气瓶生命周期
        recordLifecycle(cylinder.getId(), myCompanyId, LifecycleEventEnum.TRANSFER_OUT, "扫码入库", AccountType.APP);
        
        // 6. 更新气瓶主表：【核心控制权正式移交！】
        Cylinder updateObj = new Cylinder();
        updateObj.setId(cylinder.getId());
        updateObj.setCurrentStatus(CylinderStatus.IN_STOCK);              // 1: 恢复为【在库】状态
        updateObj.setCurrentCompanyId(myCompanyId); // 【变更】当前实际控制人变成我！
        updateObj.setOwnerPath(myCompanyPath);      // 【变更】树形归属路径也变成我！
        updateObj.setLastFlowId(flowId);            // 更新最后一次流转记录ID，提升后续查询效率
        this.baseMapper.updateById(updateObj);
    }
    
    /**
     * 私有辅助方法：提取重复的【黑匣子记录】代码
     */
    private void recordScan(Long cylinderId, Long userId, Long companyId, Integer scanType) {
        ScanRecord scan = new ScanRecord();
        scan.setCylinderId(cylinderId);
        scan.setUserId(userId);
        scan.setCompanyId(companyId);
        scan.setScanType(scanType);
        scan.setScanTime(new Date());
        scanRecordMapper.insert(scan);
    }
    
    /**
     * 私有辅助方法：提取重复的【生命周期轨迹】代码
     */
    private Long recordFlow(Long cylinderId, Long fromCompanyId, Long toCompanyId,
                            Integer flowType, Long operatorId, String remark) {
        CylinderFlow flow = new CylinderFlow();
        flow.setCylinderId(cylinderId);
        flow.setFromCompanyId(fromCompanyId);
        flow.setToCompanyId(toCompanyId);
        flow.setType(flowType);
        flow.setOperatorId(operatorId);
        flow.setRemark(remark);
        cylinderFlowMapper.insert(flow);
        
        // 同步回写主表最后一次流转ID (提升后续轨迹查询效率)
        Cylinder updateObj = new Cylinder();
        updateObj.setId(cylinderId);
        updateObj.setLastFlowId(flow.getId());
        this.baseMapper.updateById(updateObj);
        
        return flow.getId();
    }
    
    
    /**
     * ================= 扫码充装 (极限风控) =================
     */
    @Transactional(rollbackFor = Exception.class)
    public void fillCylinder(CylinderFillDto dto) {
        Long myUserId = SecurityUtils.getUserId();
        Long myCompanyId = SecurityUtils.getCompanyId();
        
        // 1. 获取当前安全上下文，再次硬性校验企业资质
        if (SecurityUtils.getCurrentUser().getTypeFiller() != 1) {
            throw new BusinessException(403, "严重违规：您所在的企业没有【充装资质】，禁止充气！");
        }
        
        // 2. 查出气瓶物理档案
        Cylinder cylinder = this.baseMapper.selectOne(new LambdaQueryWrapper<Cylinder>()
                .eq(Cylinder::getCode, dto.getQrcode()));
        if (cylinder == null) {
            throw new BusinessException(404, "未找到该气瓶信息，请核对二维码！");
        }
        
        // 3. 【绝对防线】校验控制权、状态、超期情况 (同上，保持不变)
        if (!cylinder.getCurrentCompanyId().equals(myCompanyId)) {
            throw new BusinessException(403, "产权异常：该气瓶当前不属于本充气站！");
        }
        if (ObjectUtil.equals(cylinder.getCurrentStatus(), CylinderStatus.IN_STOCK)) {
            throw new BusinessException(400, "该气瓶不处于【在库】状态！");
        }
        Date today = new Date();
        if (cylinder.getNextInspectionDate() != null && cylinder.getNextInspectionDate().before(today)) {
            lockCylinderStatus(cylinder.getId(), CylinderStatus.WAIT_INSPECT); // 锁定为待检
            throw new BusinessException(500, "🚨 报警：该气瓶已过检验有效期，严禁充装！");
        }
        
        // ==========================================
        // 4. 插入【专属定制充气记录表】
        // ==========================================
        CylinderFillRecord fillRecord = new CylinderFillRecord();
        fillRecord.setCylinderId(cylinder.getId());
        fillRecord.setCompanyId(myCompanyId);
        fillRecord.setOperatorId(myUserId);
        fillRecord.setFillWeight(dto.getFillWeight());
        fillRecord.setFillPressure(dto.getFillPressure());
        
        // 🏆 极限风控：气瓶本体是什么介质，就默认充装的是什么气体，坚决防混充！
        fillRecord.setGasType(GasType.CNG);
        
        // fillTime 作为分区键，必须极其精准
        fillRecord.setFillTime(today);
        // createTime 由 @TableField(fill = FieldFill.INSERT) 自动填充，无需手动 set
        
        cylinderFillRecordMapper.insert(fillRecord);
        
        // ==========================================
        // 5. 记录物理扫描流水与生命周期流转 (对接之前的逻辑)
        // ==========================================
        recordScan(cylinder.getId(), myUserId, myCompanyId, 4); // 4: 充装
        Long flowId = recordFlow(cylinder.getId(), myCompanyId, myCompanyId, 4, myUserId,
                "充装完成。压力: " + dto.getFillPressure() + "，净重: " + dto.getFillWeight() + "kg");
        
        // ==========================================
        // 6. 记录系统级防黑客操作日志
        // ==========================================
        recordOperationLog(OperationType.INFLATE, TargetType.CYLINDER,  cylinder.getId());
        
        // 记录气瓶生命周期
        recordLifecycle(cylinder.getId(), myCompanyId, LifecycleEventEnum.FILL, "扫码充气", AccountType.APP);
        
        // ==========================================
        // 7. 更新气瓶主表状态
        // ==========================================
        Cylinder updateObj = new Cylinder();
        updateObj.setId(cylinder.getId());
        updateObj.setLastFillTime(today);
        // 只要充过气，不管是流转还是充装，最后操作的流水ID都要更新上去，大屏查询才能极速响应
        updateObj.setLastFlowId(flowId);
        this.baseMapper.updateById(updateObj);
    }
    
    /**
     * 辅助方法：触碰安全红线时，强制锁定气瓶状态
     */
    private void lockCylinderStatus(Long cylinderId, CylinderStatus targetStatus) {
        Cylinder lockObj = new Cylinder();
        lockObj.setId(cylinderId);
        lockObj.setCurrentStatus(targetStatus);
        this.baseMapper.updateById(lockObj);
    }
    
    /**
     * 工业级辅助方法 1：记录气瓶生命周期轨迹
     */
    private void recordLifecycle(Long cylinderId, Long companyId, LifecycleEventEnum eventEnum, String remark, AccountType accountType) {
        CylinderLifecycle lifecycle = new CylinderLifecycle();
        lifecycle.setCylinderId(cylinderId);
        lifecycle.setEventType(eventEnum);
        lifecycle.setCompanyId(companyId);
        lifecycle.setOperatorId(SecurityUtils.getUserId());
        lifecycle.setAccountType(accountType);
        lifecycle.setEventTime(new Date());
        lifecycle.setRemark(remark);
        
        cylinderLifecycleService.save(lifecycle);
    }
    
    /**
     * 工业级辅助方法 2：记录系统级防篡改操作日志
     */
    private void recordOperationLog(OperationType operation, TargetType targetType, Long targetId) {
        OperationLog log = new OperationLog();
        log.setUserId(SecurityUtils.getUserId());
        log.setOperation(operation);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        
        // 获取当前请求的真实 IP
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                // 如果有 Nginx 代理，请从 "X-Forwarded-For" 头获取
                log.setIp(request.getRemoteAddr());
            }
        } catch (Exception e) {
            log.setIp("Unknown");
        }
        
        // createTime 由 @TableField(fill = FieldFill.INSERT) 自动处理
        operationLogMapper.insert(log);
    }
    
    
    /**
     * 管理端查询气瓶数据
     * @param req
     * @return
     */
    public PageResult<CylinderPageDto> queryCylinderPage(CylinderQueryReq req) {
        // ==========================================
        // 1. 【核心：数据权限隔离】
        // ==========================================
        Long myCompanyId = SecurityUtils.getCompanyId();
        Boolean isAdmin = SecurityUtils.getCurrentUser().getUser().getIsAdmin();
        Company company = companyMapper.selectById(myCompanyId);
        boolean isManufacturer = ObjectUtil.equals(company.getTypeManufacturer(), 1);// 是否制造商
        
        LambdaQueryWrapper<Cylinder> wrapper = new LambdaQueryWrapper<>();
        
        // 如果不是制造商管理员，必须把查询范围死死锁在【本企业】及其下属机构
        if (!isAdmin || !isManufacturer) {
            // 这里有两种策略，最严谨的是：当前在我手里的，或者 ownerPath 包含我企业的
            wrapper.and(w -> w.eq(Cylinder::getCurrentCompanyId, myCompanyId)
                              .or()
                              .like(Cylinder::getOwnerPath, "," + myCompanyId + ","));
        } else {
            // 超级管理员可以通过前端下拉框筛选指定企业
            if (req.getCurrentCompanyId() != null) {
                wrapper.eq(Cylinder::getCurrentCompanyId, req.getCurrentCompanyId());
            }
        }
        
        // ==========================================
        // 2. 动态拼装复合查询条件
        // ==========================================
        // 二维码精确匹配 (由于是主键性质，建议 eq 而不是 like，效率极高)
        if (StrUtil.isNotBlank(req.getCode())) {
            wrapper.eq(Cylinder::getCode, req.getCode().trim());
        }
        // 规格查询
        if (StrUtil.isNotBlank(req.getSpec())) {
            wrapper.eq(Cylinder::getSpec, req.getSpec());
        }
        // 状态精准查询
        if (req.getCurrentStatus() != null) {
            wrapper.eq(Cylinder::getCurrentStatus, req.getCurrentStatus());
        }
        // 制造商筛选
        if (req.getManufacturerId() != null) {
            wrapper.eq(Cylinder::getManufacturerId, req.getManufacturerId());
        }
        // 🚨 超期预警区间查询 (重点：特检局最爱看的数据！)
        if (req.getNextInspectDateStart() != null) {
            wrapper.ge(Cylinder::getNextInspectionDate, req.getNextInspectDateStart());
        }
        if (req.getNextInspectDateEnd() != null) {
            wrapper.le(Cylinder::getNextInspectionDate, req.getNextInspectDateEnd());
        }
        
        // 默认按创建时间倒序排
        wrapper.orderByDesc(Cylinder::getCreateTime);
        
        // ==========================================
        // 3. 执行纯粹的物理分页查询
        // ==========================================
        Page<Cylinder> page = new Page<>(req.getPage(), req.getSize());
        this.baseMapper.selectPage(page, wrapper);
        
        List<Cylinder> records = page.getRecords();
        if (CollUtil.isEmpty(records)) {
            return new PageResult<>(new ArrayList<>(), page.getTotal());
        }
        
        // ==========================================
        // 4. 【高阶技巧：内存中批量翻译企业名称，拒绝 N+1】
        // ==========================================
        // 提取当前页所有涉及到的 企业ID (包括 currentCompanyId 和 manufacturerId)
        Set<Long> companyIds = new HashSet<>();
        records.forEach(c -> {
            if (c.getCurrentCompanyId() != null) companyIds.add(c.getCurrentCompanyId());
            if (c.getManufacturerId() != null) companyIds.add(c.getManufacturerId());
        });
        
        // IN 查询一次性把本页涉及到的企业全部查出来，转为 Map
        Map<Long, String> companyNameMap = new HashMap<>();
        if (CollUtil.isNotEmpty(companyIds)) {
            List<Company> companies = companyMapper.selectList(
                    new LambdaQueryWrapper<Company>()
                            .in(Company::getId, companyIds)
                            .select(Company::getId, Company::getName) // 性能优化：只查 ID 和 Name
            );
            companyNameMap = companies.stream()
                                      .collect(Collectors.toMap(Company::getId, Company::getName));
        }
        
        // ==========================================
        // 5. 组装最终 DTO 列表
        // ==========================================
        Map<Long, String> finalCompanyNameMap = companyNameMap;
        List<CylinderPageDto> dtoList = records.stream().map(cylinder -> {
            CylinderPageDto dto = new CylinderPageDto();
            BeanUtils.copyProperties(cylinder, dto);
            
            // 从 Map 中极速回显名称
            dto.setCurrentCompanyName(finalCompanyNameMap.get(cylinder.getCurrentCompanyId()));
            dto.setManufacturerName(finalCompanyNameMap.get(cylinder.getManufacturerId()));
            
            return dto;
        }).collect(Collectors.toList());
        
        return new PageResult<>(dtoList, page.getTotal());
    }
    
    
    /**
     * 管理端查询气瓶详情（含生命周期轨迹）
     * @param id
     * @return
     */
    public CylinderDetailDto getCylinderDetail(Long id) {
        // ==========================================
        // 1. 查询主表并进行极其严格的数据越权拦截
        // ==========================================
        Cylinder cylinder = this.baseMapper.selectById(id);
        if (cylinder == null) {
            throw new BusinessException(404, "未找到该气瓶档案！");
        }
        
        Long myCompanyId = SecurityUtils.getCompanyId();
        Boolean isAdmin = SecurityUtils.getCurrentUser().getUser().getIsAdmin();
        
        // 极限防越权：如果你不是超级管理员，你只能看：
        // 1. 你自己造的瓶子 (manufacturerId)
        // 2. 当前在你手里的瓶子 (currentCompanyId)
        // 3. 曾经经过你手的瓶子 (ownerPath 包含你)
        if (!isAdmin) {
            boolean isMine = cylinder.getCurrentCompanyId().equals(myCompanyId) ||
                    cylinder.getManufacturerId().equals(myCompanyId) ||
                    (cylinder.getOwnerPath() != null && cylinder.getOwnerPath().contains("," + myCompanyId + ","));
            if (!isMine) {
                throw new BusinessException(403, "无权查看！该气瓶既不属于您的网点，也未流经过您的网点。");
            }
        }
        
        CylinderDetailDto dto = new CylinderDetailDto();
        BeanUtils.copyProperties(cylinder, dto);
        
        // ==========================================
        // 2. 查出它所有的生命周期轨迹 (按时间倒序排，最新的在最上面)
        // ==========================================
        List<CylinderLifecycle> lifecycles = cylinderLifecycleService.list(
                new LambdaQueryWrapper<CylinderLifecycle>()
                        .eq(CylinderLifecycle::getCylinderId, cylinder.getId())
                        .orderByDesc(CylinderLifecycle::getEventTime)
        );
        
        // ==========================================
        // 3. 【极速映射准备】分别收集两套用户体系的 ID
        // ==========================================
        Set<Long> companyIdsToFetch = new HashSet<>();
        companyIdsToFetch.add(cylinder.getManufacturerId());
        companyIdsToFetch.add(cylinder.getCurrentCompanyId());
        
        Set<Long> appUserIdsToFetch = new HashSet<>();
        Set<Long> adminUserIdsToFetch = new HashSet<>();
        
        if (CollUtil.isNotEmpty(lifecycles)) {
            lifecycles.forEach(lc -> {
                if (lc.getCompanyId() != null) {
                    companyIdsToFetch.add(lc.getCompanyId());
                }
                
                // 核心逻辑：根据 accountType 将 ID 分流到不同的 Set 中
                if (lc.getOperatorId() != null && lc.getAccountType() != null) {
                    if (AccountType.APP.equals(lc.getAccountType())) {
                        appUserIdsToFetch.add(lc.getOperatorId());
                    } else if (AccountType.ADMIN.equals(lc.getAccountType())) {
                        adminUserIdsToFetch.add(lc.getOperatorId());
                    }
                }
            });
        }
        
        // ==========================================
        // 4. 批量执行 IN 查询，获取字典 Map (拒绝 N+1 连表)
        // ==========================================
        Map<Long, String> companyNameMap = new HashMap<>();
        if (CollUtil.isNotEmpty(companyIdsToFetch)) {
            List<Company> companies = companyMapper.selectBatchIds(companyIdsToFetch);
            companyNameMap = companies.stream().collect(Collectors.toMap(Company::getId, Company::getName));
        }
        
        // 4.1 翻译 APP 端用户
        Map<Long, String> appUserNameMap = new HashMap<>();
        if (CollUtil.isNotEmpty(appUserIdsToFetch)) {
            List<AppUser> appUsers = appUserMapper.selectBatchIds(appUserIdsToFetch);
            appUserNameMap = appUsers.stream().collect(Collectors.toMap(AppUser::getId, AppUser::getUsername));
        }
        
        // 4.2 翻译 管理端用户 (假设实体名为 User)
        Map<Long, String> adminUserNameMap = new HashMap<>();
        if (CollUtil.isNotEmpty(adminUserIdsToFetch)) {
            List<User> adminUsers = sysUserMapper.selectBatchIds(adminUserIdsToFetch);
            adminUserNameMap = adminUsers.stream().collect(Collectors.toMap(User::getId, User::getUsername));
        }
        
        // ==========================================
        // 5. 补充主表翻译字段 (企业名、批次号)
        // ==========================================
        dto.setManufacturerName(companyNameMap.get(cylinder.getManufacturerId()));
        dto.setCurrentCompanyName(companyNameMap.get(cylinder.getCurrentCompanyId()));
        
        if (cylinder.getBatchId() != null) {
            CylinderBatch batch = cylinderBatchMapper.selectById(cylinder.getBatchId());
            if (batch != null) {
                dto.setBatchNo(batch.getBatchNo());
            }
        }
        
        // ==========================================
        // 6. 拼装时间轴节点
        // ==========================================
        List<CylinderDetailDto.LifecycleNode> timeline = new ArrayList<>();
        if (CollUtil.isNotEmpty(lifecycles)) {
            for (CylinderLifecycle lc : lifecycles) {
                CylinderDetailDto.LifecycleNode node = new CylinderDetailDto.LifecycleNode();
                
                node.setEventName(lc.getEventType() != null ? lc.getEventType().getName() : "未知操作");
                node.setEventTime(lc.getEventTime());
                node.setCompanyName(companyNameMap.getOrDefault(lc.getCompanyId(), "未知网点"));
                
                // 核心逻辑：根据 accountType 从对应的 Map 中获取精准的人名
                String operatorName = "系统/未知用户";
                if (lc.getOperatorId() != null && lc.getAccountType() != null) {
                    if (AccountType.APP.equals(lc.getAccountType())) {
                        operatorName = appUserNameMap.getOrDefault(lc.getOperatorId(), "未知APP用户") + " (APP端)";
                    } else if (AccountType.ADMIN.equals(lc.getAccountType())) {
                        operatorName = adminUserNameMap.getOrDefault(lc.getOperatorId(), "未知管理员") + " (管理端)";
                    }
                }
                node.setOperatorName(operatorName);
                
                node.setRemark(lc.getRemark());
                
                timeline.add(node);
            }
        }
        dto.setTimeline(timeline);
        
        return dto;
    }
    
    
}
