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

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.exception.BusinessException;
import me.zhengjie.modules.maint.domain.cylinder.entity.*;
import me.zhengjie.modules.maint.domain.cylinder.mapper.*;
import me.zhengjie.modules.maint.domain.dto.CylinderFillDto;
import me.zhengjie.modules.maint.domain.dto.CylinderFlowDto;
import me.zhengjie.modules.maint.domain.enums.*;
import me.zhengjie.modules.maint.util.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;

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
    
    /**
     * ================= 扫码出库 (发货) =================
     */
    @Transactional(rollbackFor = Exception.class)
    public void scanOut(CylinderFlowDto dto) {
        Long myUserId = SecurityUtils.getUserId();
        Long myCompanyId = SecurityUtils.getCompanyId();
        
        // 1. 查出气瓶
        Cylinder cylinder = this.baseMapper.selectOne(new LambdaQueryWrapper<Cylinder>()
                .eq(Cylinder::getQrcode, dto.getQrcode()));
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
        recordOperationLog(
                OperationType.OUT,
                TargetType.CYLINDER,  // 目标对象是气瓶
                cylinder.getId()
        );
        
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
                .eq(Cylinder::getQrcode, dto.getQrcode()));
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
        recordOperationLog(
                OperationType.IN,
                TargetType.CYLINDER,  // 目标对象是气瓶
                cylinder.getId()
        );
        
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
                .eq(Cylinder::getQrcode, dto.getQrcode()));
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
        recordOperationLog(
                OperationType.INFLATE,
                TargetType.CYLINDER,  // 目标对象是气瓶
                cylinder.getId()
        );
        
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
    
    
    private final CylinderLifecycleMapper cylinderLifecycleMapper;
    private final OperationLogMapper operationLogMapper;
    
    /**
     * 工业级辅助方法 1：记录气瓶生命周期轨迹
     */
    private void recordLifecycle(Long cylinderId, Long companyId, LifecycleEventEnum eventEnum, String remark) {
        CylinderLifecycle lifecycle = new CylinderLifecycle();
        lifecycle.setCylinderId(cylinderId);
        lifecycle.setEventType(eventEnum);
        lifecycle.setCompanyId(companyId);
        lifecycle.setOperatorId(SecurityUtils.getUserId());
        lifecycle.setEventTime(new Date());
        lifecycle.setRemark(remark);
        
        cylinderLifecycleMapper.insert(lifecycle);
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
    
}
