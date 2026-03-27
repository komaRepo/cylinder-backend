package me.zhengjie.modules.maint.util;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Data;
import me.zhengjie.exception.BusinessException;

import java.math.BigDecimal;
import java.util.Date;

public class RfidParserUtil {
    
    /**
     * 解析并校验 64字节(包含逗号) 的 RFID 原始数据
     * @return 包含全量国标信息的超级聚合对象 RfidLabel
     */
    public static RfidLabel parse(String rawData) {
        if (StrUtil.isBlank(rawData)) {
            throw new BusinessException(400, "RFID标签数据为空");
        }
        
        // 1. 【第一道绝对防线】校验总长度
        if (rawData.length() != 64) {
            throw new BusinessException(400, "RFID报文总长度异常：预期64位，实际" + rawData.length() + "位");
        }
        
        // 2. 按逗号切割
        String[] parts = rawData.split(",");
        if (parts.length != 6) {
            throw new BusinessException(400, "RFID标签格式非法，必须包含5个逗号分隔符");
        }
        
        String version  = parts[0];
        String uniqueId = parts[1];
        String mfgExt   = parts[2];
        String salesExt = parts[3];
        String reserved = parts[4];
        String crc      = parts[5];
        
        // 3. 【第二道绝对防线】严格校验每一个数据块的长度
        if (version.length() != 1 || uniqueId.length() != 16 || mfgExt.length() != 16 ||
                salesExt.length() != 16 || reserved.length() != 6 || crc.length() != 4) {
            throw new BusinessException(400, "RFID内部数据段长度遭到破坏，不符合国标规范");
        }
        
        // ==========================================
        // 4. 【防篡改核心】CRC-16-CCITT 校验 (预留)
        // ==========================================
        String dataToVerify = rawData.substring(0, 59);
        // String calculatedCrc = Crc16Util.getCRC16CCITT(dataToVerify);
        // if (!calculatedCrc.equalsIgnoreCase(crc)) {
        //     throw new BusinessException(400, "RFID校验码验证失败，疑似非法伪造标签！");
        // }
        
        try {

            RfidLabel label = new RfidLabel();
            label.setVersion(version);
            label.setReserved(reserved);
            label.setCrc(crc);
            
            // 5.1 解析模块一：制造唯一性编号 (16位)
            label.setUniqueInfo(parseUniqueId(uniqueId));
            
            // 5.2 解析模块二：制造商信息扩展 (16位)
            label.setMfgExtInfo(parseMfgExt(mfgExt, label.getUniqueInfo().getYearStr()));
            
            // 5.3 解析模块三：销售充装商信息 (16位)
            label.setSalesFillerInfo(parseSalesFillerExt(salesExt));
            
            return label;
            
        } catch (Exception e) {
            throw new BusinessException(400, "RFID内部数据解析异常: " + e.getMessage());
        }
    }
    
    /**
     * 模块一解析：制造唯一性编号
     */
    private static UniqueIdInfo parseUniqueId(String uniqueId) {
        UniqueIdInfo info = new UniqueIdInfo();
        info.setFullCode(uniqueId);
        info.setCategoryCode(uniqueId.substring(0, 1)); // 气瓶品种码
        info.setProvinceCode(uniqueId.substring(1, 3)); // 制造单位所在地(行政区划)
        info.setMfgCode(uniqueId.substring(3, 7));      // 制造单位代码(4位)
        info.setYearStr(uniqueId.substring(7, 9));      // 制造登记年份(后2位)
        info.setSerialNo(uniqueId.substring(9, 16));    // 产品序号(7位)
        return info;
    }
    
    /**
     * 模块二解析：制造商信息扩展
     */
    private static MfgExtInfo parseMfgExt(String mfgExt, String yearStr) {
        MfgExtInfo info = new MfgExtInfo();
        info.setMonthStr(mfgExt.substring(0, 2));             // 制造月份
        info.setBatchNo(mfgExt.substring(2, 6));              // 生产批号
        info.setDesignLifeYears(Integer.parseInt(mfgExt.substring(6, 8))); // 设计使用年限
        info.setVolume(new BigDecimal(mfgExt.substring(8, 11)));           // 公称水容积
        info.setModelCode(mfgExt.substring(11, 16).trim());   // 气瓶型号(去空格)
        
        // 业务计算：组装完整出厂日期
        int fullYear = 2000 + Integer.parseInt(yearStr);
        String dateString = String.format("%04d-%s-01", fullYear, info.getMonthStr());
        Date produceDate = DateUtil.parseDate(dateString);
        info.setProduceDate(produceDate);
        
        // 业务计算：下次检验日期 (出厂日期 + 设计年限)
        int lifeMonths = info.getDesignLifeYears() * 12;
        info.setNextInspectDate(DateUtil.offsetMonth(produceDate, lifeMonths));
        return info;
    }
    
    /**
     * 模块三解析：销售充装商信息
     */
    private static SalesFillerExtInfo parseSalesFillerExt(String salesExt) {
        SalesFillerExtInfo info = new SalesFillerExtInfo();
        info.setSalesProvinceCode(salesExt.substring(0, 2)); // 销售单位所在地
        info.setSalesCode(salesExt.substring(2, 6));         // 销售单位代码
        info.setSalesTimeStr(salesExt.substring(6, 10));     // 销售时间 (年月)
        info.setFillerProvinceCode(salesExt.substring(10, 12)); // 充装单位所在地
        info.setFillerCode(salesExt.substring(12, 16));      // 充装单位代码
        return info;
    }
    
    // =====================================================================
    // 下方是极度舒适的领域对象定义 (Data Objects)
    // =====================================================================
    
    /**
     * 根对象：RFID 完整标签映射
     */
    @Data
    public static class RfidLabel {
        private String version;                 // 版本号 (1字节)
        private UniqueIdInfo uniqueInfo;        // 制造唯一性编号模块
        private MfgExtInfo mfgExtInfo;          // 制造商信息扩展模块
        private SalesFillerExtInfo salesFillerInfo; // 销售充装商信息模块
        private String reserved;                // 预留信息 (6字节)
        private String crc;                     // 校验码 (4字节)
    }
    
    /**
     * 模块一：制造唯一性编号 (4.1.2)
     */
    @Data
    public static class UniqueIdInfo {
        private String fullCode;     // 16位完整系统主键码 (即气瓶二维码)
        private String categoryCode; // 气瓶品种码 (1位，如:1=无缝气瓶)
        private String provinceCode; // 制造单位所在地行政区划 (2位，如:03)
        private String mfgCode;      // 制造单位代码 (4位，监管分配)
        private String yearStr;      // 制造登记年份后两位 (2位，如:26)
        private String serialNo;     // 产品序号 (7位厂商流水号)
    }
    
    /**
     * 模块二：制造商信息扩展 (4.1.3)
     */
    @Data
    public static class MfgExtInfo {
        private String monthStr;         // 制造月份 (2位，01~12)
        private String batchNo;          // 生产批号 (4位)
        private Integer designLifeYears; // 设计使用年限 (2位)
        private BigDecimal volume;       // 公称水容积 (3位，升)
        private String modelCode;        // 气瓶型号代码 (5位)
        
        // --- 衍生业务字段 ---
        private Date produceDate;        // 精准计算出的：出厂日期
        private Date nextInspectDate;    // 精准计算出的：下次检验日期
    }
    
    /**
     * 模块三：销售充装商信息 (4.1.4)
     */
    @Data
    public static class SalesFillerExtInfo {
        private String salesProvinceCode;  // 销售单位所在地 (2位)
        private String salesCode;          // 销售单位代码 (4位)
        private String salesTimeStr;       // 销售时间 (4位，YYMM)
        private String fillerProvinceCode; // 充装单位所在地 (2位)
        private String fillerCode;         // 充装单位代码 (4位)
    }
}