package me.zhengjie.modules.maint.domain.cylinder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.modules.maint.domain.dto.RegionTreeDto;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
@Service
public class RegionGlobalService {
    
    private final static String COUNTRY_FILE = "data/countries.json";
    private final static String STATE_FILE = "data/states.json";
    private final static String CITY_FILE = "data/cities.json";
    
    // ===== 基础数据 =====
    private int[] countryIds;
    private String[] countryNames;
    
    private int[] stateIds;
    private String[] stateNames;
    private int[] stateCountryIds;
    
    private int[] cityIds;
    private String[] cityNames;
    private int[] cityStateIds;
    
    // ===== 索引 =====
    private Map<Integer, int[]> countryStateIndex = new HashMap<>();
    private Map<Integer, int[]> stateCityIndex = new HashMap<>();
    
    // ===== 多语言缓存 =====
    private final Map<String, LanguagePack> langCache = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void init() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            
            List<Map<String, Object>> countries = mapper.readValue(
                    new ClassPathResource(COUNTRY_FILE).getInputStream(),
                    new TypeReference<List<Map<String, Object>>>() {}
            );
            
            List<Map<String, Object>> states = mapper.readValue(
                    new ClassPathResource(STATE_FILE).getInputStream(),
                    new TypeReference<List<Map<String, Object>>>() {}
            );
            
            List<Map<String, Object>> cities = mapper.readValue(
                    new ClassPathResource(CITY_FILE).getInputStream(),
                    new TypeReference<List<Map<String, Object>>>() {}
            );
            
            initCountries(countries);
            initStates(states);
            initCities(cities);
            
            buildIndex();
            
            log.info("✅ 全球地区数据加载完成：国家 {}，州 {}，城市 {}",
                    countryIds.length, stateIds.length, cityIds.length);
            
        } catch (Exception e) {
            log.error("❌ 初始化失败", e);
        }
    }
    
    private void initCountries(List<Map<String, Object>> list) {
        int size = list.size();
        countryIds = new int[size];
        countryNames = new String[size];
        
        for (int i = 0; i < size; i++) {
            Map<String, Object> c = list.get(i);
            countryIds[i] = (Integer) c.get("id");
            countryNames[i] = (String) c.get("name");
        }
    }
    
    private void initStates(List<Map<String, Object>> list) {
        int size = list.size();
        stateIds = new int[size];
        stateNames = new String[size];
        stateCountryIds = new int[size];
        
        for (int i = 0; i < size; i++) {
            Map<String, Object> s = list.get(i);
            stateIds[i] = (Integer) s.get("id");
            stateNames[i] = (String) s.get("name");
            stateCountryIds[i] = (Integer) s.get("country_id");
        }
    }
    
    private void initCities(List<Map<String, Object>> list) {
        int size = list.size();
        cityIds = new int[size];
        cityNames = new String[size];
        cityStateIds = new int[size];
        
        for (int i = 0; i < size; i++) {
            Map<String, Object> c = list.get(i);
            cityIds[i] = (Integer) c.get("id");
            cityNames[i] = (String) c.get("name");
            cityStateIds[i] = (Integer) c.get("state_id");
        }
    }
    
    private void buildIndex() {
        
        Map<Integer, List<Integer>> tempStateMap = new HashMap<>();
        
        for (int i = 0; i < stateIds.length; i++) {
            tempStateMap
                    .computeIfAbsent(stateCountryIds[i], k -> new ArrayList<>())
                    .add(i);
        }
        
        for (Map.Entry<Integer, List<Integer>> e : tempStateMap.entrySet()) {
            countryStateIndex.put(
                    e.getKey(),
                    e.getValue().stream().mapToInt(Integer::intValue).toArray()
            );
        }
        
        Map<Integer, List<Integer>> tempCityMap = new HashMap<>();
        
        for (int i = 0; i < cityIds.length; i++) {
            tempCityMap
                    .computeIfAbsent(cityStateIds[i], k -> new ArrayList<>())
                    .add(i);
        }
        
        for (Map.Entry<Integer, List<Integer>> e : tempCityMap.entrySet()) {
            stateCityIndex.put(
                    e.getKey(),
                    e.getValue().stream().mapToInt(Integer::intValue).toArray()
            );
        }
    }
    
    // =========================
    // 多语言
    // =========================
    
    private static class LanguagePack {
        private final String[] countryNames;
        private final String[] stateNames;
        private final String[] cityNames;
        
        public LanguagePack(String[] countryNames, String[] stateNames, String[] cityNames) {
            this.countryNames = countryNames;
            this.stateNames = stateNames;
            this.cityNames = cityNames;
        }
        
        public String[] getCountryNames() {
            return countryNames;
        }
        
        public String[] getStateNames() {
            return stateNames;
        }
        
        public String[] getCityNames() {
            return cityNames;
        }
    }
    
    private LanguagePack loadLanguage(String lang) {
        
        return langCache.computeIfAbsent(lang, l -> {
            try {
                ObjectMapper mapper = new ObjectMapper();
                
                log.info("🌍 加载语言: {}", l);
                
                String[] countryTrans = buildTranslationArray(
                        countryNames,
                        mapper.readValue(
                                new ClassPathResource(COUNTRY_FILE).getInputStream(),
                                new TypeReference<List<Map<String, Object>>>() {}
                        ),
                        l
                );
                
                String[] stateTrans = buildTranslationArray(
                        stateNames,
                        mapper.readValue(
                                new ClassPathResource(STATE_FILE).getInputStream(),
                                new TypeReference<List<Map<String, Object>>>() {}
                        ),
                        l
                );
                
                String[] cityTrans = buildTranslationArray(
                        cityNames,
                        mapper.readValue(
                                new ClassPathResource(CITY_FILE).getInputStream(),
                                new TypeReference<List<Map<String, Object>>>() {}
                        ),
                        l
                );
                
                return new LanguagePack(countryTrans, stateTrans, cityTrans);
                
            } catch (Exception e) {
                log.error("❌ 加载语言失败", e);
                return new LanguagePack(countryNames, stateNames, cityNames);
            }
        });
    }
    
    private String[] buildTranslationArray(
            String[] baseNames,
            List<Map<String, Object>> transList,
            String lang) {
        
        Map<String, String> map = new HashMap<>();
        
        for (Map<String, Object> item : transList) {
            String name = (String) item.get("name");
            Map<String, String> translations =
                    (Map<String, String>) item.get("translations");
            
            if (translations != null && translations.containsKey(lang)) {
                map.put(name, translations.get(lang));
            }
        }
        
        String[] result = new String[baseNames.length];
        
        for (int i = 0; i < baseNames.length; i++) {
            result[i] = map.getOrDefault(baseNames[i], baseNames[i]);
        }
        
        return result;
    }
    
    // =========================
    // 对外接口
    // =========================
    
    public List<RegionTreeDto> getCountries(String lang) {
        
        LanguagePack pack = loadLanguage(lang);
        
        List<RegionTreeDto> list = new ArrayList<>(countryIds.length);
        
        for (int i = 0; i < countryIds.length; i++) {
            RegionTreeDto dto = new RegionTreeDto();
            dto.setValue(String.valueOf(countryIds[i]));
            dto.setLabel(pack.getCountryNames()[i]);
            list.add(dto);
        }
        
        return list;
    }
    
    public List<RegionTreeDto> getStates(int countryId, String lang) {
        
        LanguagePack pack = loadLanguage(lang);
        
        int[] idxArr = countryStateIndex.getOrDefault(countryId, new int[0]);
        
        List<RegionTreeDto> list = new ArrayList<>(idxArr.length);
        
        for (int idx : idxArr) {
            RegionTreeDto dto = new RegionTreeDto();
            dto.setValue(String.valueOf(stateIds[idx]));
            dto.setLabel(pack.getStateNames()[idx]);
            list.add(dto);
        }
        
        return list;
    }
    
    public List<RegionTreeDto> getCities(int stateId, String lang) {
        
        LanguagePack pack = loadLanguage(lang);
        
        int[] idxArr = stateCityIndex.getOrDefault(stateId, new int[0]);
        
        List<RegionTreeDto> list = new ArrayList<>(idxArr.length);
        
        for (int idx : idxArr) {
            RegionTreeDto dto = new RegionTreeDto();
            dto.setValue(String.valueOf(cityIds[idx]));
            dto.setLabel(pack.getCityNames()[idx]);
            list.add(dto);
        }
        
        return list;
    }
}