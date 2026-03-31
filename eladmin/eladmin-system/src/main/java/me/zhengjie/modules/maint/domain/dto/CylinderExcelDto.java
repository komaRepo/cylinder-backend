package me.zhengjie.modules.maint.domain.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class CylinderExcelDto {
    
    @ExcelProperty(index = 0)
    private String rfidRawData;
}