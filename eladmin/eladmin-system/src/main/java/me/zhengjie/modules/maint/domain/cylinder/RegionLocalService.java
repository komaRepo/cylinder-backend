package me.zhengjie.modules.maint.domain.cylinder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.modules.maint.domain.dto.RegionTreeDto;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class RegionLocalService {

    // 内存级缓存，启动时加载一次，永久驻留内存
    private List<RegionTreeDto> regionTreeCache = new ArrayList<>();
    
    private Map<String, String> provinceNameToCodeMap = new HashMap<>();

    /**
     * @PostConstruct 注解表示在 Spring Bean 初始化完成后自动执行该方法
     */
    @PostConstruct
    public void init() {
        try {
            ClassPathResource resource = new ClassPathResource("data/region.json");
            InputStream inputStream = resource.getInputStream();
            
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Map<String, String>> rawData = mapper.readValue(
                    inputStream,
                    new TypeReference<Map<String, Map<String, String>>>() {}
            );
            
            // 【新增】：提前把省份名称和代码的反向关系存入内存 HashMap
            Map<String, String> provinces = rawData.get("86");
            if (provinces != null) {
                for (Map.Entry<String, String> entry : provinces.entrySet()) {
                    // key: 440000, value: 广东省
                    provinceNameToCodeMap.put(entry.getValue(), entry.getKey());
                }
            }
            
            this.regionTreeCache = buildTree(rawData);
            
            log.info("✅ 本地省市区数据加载成功！共加载 {} 个省级行政区", regionTreeCache.size());
        } catch (Exception e) {
            log.error("❌ 读取本地省市区 JSON 失败！", e);
        }
    }

    /**
     * 将平铺的字典数据组装为标准的层级树
     */
    private List<RegionTreeDto> buildTree(Map<String, Map<String, String>> rawData) {
        List<RegionTreeDto> tree = new ArrayList<>();

        // 1. 获取顶级节点 (所有的省，JSON 中标识为 "86")
        Map<String, String> provinces = rawData.get("86");
        if (provinces == null) {
            return tree;
        }

        // 2. 遍历所有省份
        for (Map.Entry<String, String> provEntry : provinces.entrySet()) {
            RegionTreeDto provNode = new RegionTreeDto();
            provNode.setValue(provEntry.getKey());
            provNode.setLabel(provEntry.getValue());
            provNode.setChildren(new ArrayList<>());

            // 3. 去大字典里捞这个省份下面的城市
            Map<String, String> cities = rawData.get(provEntry.getKey());
            if (cities != null) {
                // 遍历该省下的城市
                for (Map.Entry<String, String> cityEntry : cities.entrySet()) {
                    RegionTreeDto cityNode = new RegionTreeDto();
                    cityNode.setValue(cityEntry.getKey());
                    cityNode.setLabel(cityEntry.getValue());
                    cityNode.setChildren(new ArrayList<>());

                    // 4. 去大字典里捞这个城市下面的区县
                    Map<String, String> districts = rawData.get(cityEntry.getKey());
                    if (districts != null) {
                        for (Map.Entry<String, String> distEntry : districts.entrySet()) {
                            RegionTreeDto distNode = new RegionTreeDto();
                            distNode.setValue(distEntry.getKey());
                            distNode.setLabel(distEntry.getValue());
                            // 区县是最后一级，不实例化 children 集合
                            cityNode.getChildren().add(distNode);
                        }
                    }
                    
                    // 防呆：如果城市下面没区县，把 children 置空，防止前端渲染出空白级联
                    if (cityNode.getChildren().isEmpty()) cityNode.setChildren(null);
                    provNode.getChildren().add(cityNode);
                }
            }

            if (provNode.getChildren().isEmpty()) provNode.setChildren(null);
            tree.add(provNode);
        }

        return tree;
    }

    /**
     * 暴露给 Controller 调用的方法：直接返回内存中的缓存树
     */
    public List<RegionTreeDto> getRegionTree() {
        return this.regionTreeCache;
    }
    
    
    /**
     * 【新增】：暴露给外界调用的极速查询方法
     * @param provinceName 数据库里的省份全称 (如: "广东省")
     * @return 行政代码 (如: "440000")
     */
    public String getProvinceCodeByName(String provinceName) {
        if (provinceName == null) return null;
        return provinceNameToCodeMap.get(provinceName);
    }
    
}