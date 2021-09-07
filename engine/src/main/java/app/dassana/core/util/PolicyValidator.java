package app.dassana.core.util;

import app.dassana.core.api.ValidationException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.yaml.snakeyaml.Yaml;

import javax.inject.Singleton;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class PolicyValidator {

    private Map<String, Map<String, Map<String, List<String>>>> fieldTemplate = new HashMap<>();
    private Yaml yaml = new Yaml();

    //load yaml 1st
    public void loadYaml(String path) throws FileNotFoundException {
        fieldTemplate = yaml.load((new FileInputStream(path)));
    }


    //then load files
    public void processFiles(String path) throws IOException {
        File dir = new File(path);
        IOFileFilter fileFilter = new SuffixFileFilter(".yaml");
        List<File> files = (List<File>) FileUtils.listFiles(dir, fileFilter, TrueFileFilter.INSTANCE);
        for (File file : files) {
            if(file.getCanonicalPath().contains("policy-context")) {
                Map<String, Object> map = yaml.load(new FileInputStream(file));
                if(!isValidYaml(map)){
                    throw new ValidationException("Not a valid policy file: " + file.getAbsolutePath());
                }
            }
        }
    }

    private boolean isValidYaml(Map<String, Object> map) {
        return hasAllFields(map) && hasValidFields(map);
    }

    /*
       1. if missing class, subclass, category - INVALID
       2. if class=risk and subcategory missing - INVALID
    */
    private boolean hasAllFields(Map<String, Object> map){
        boolean hasFields = map.containsKey("class") && map.containsKey("subclass")
                && map.containsKey("category");

        if(hasFields && "risk".equals(map.get("class"))){
            hasFields = hasFields && map.containsKey("subcategory");
        }

        return hasFields;
    }

    private boolean hasValidFields(Map<String, Object> map){
        String pClass = (String) map.get("class");
        String subClass = (String) map.get("subclass");
        String category = (String) map.get("category");

        boolean isValidFields = fieldTemplate.containsKey(pClass) && fieldTemplate.get(pClass).containsKey(subClass)
                && fieldTemplate.get(pClass).get(subClass).containsKey(category);

        if("risk".equals(pClass) && isValidFields){
            String subCategory = (String) map.get("subcategory");
            isValidFields = fieldTemplate.get(pClass).get(subClass).get(category).contains(subCategory);
        }

        return isValidFields;
    }

}
