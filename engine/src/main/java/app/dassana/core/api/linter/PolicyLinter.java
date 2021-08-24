package app.dassana.core.api.linter;

import app.dassana.core.api.ValidationException;
import app.dassana.core.api.linter.pojo.*;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;

import java.io.*;
import java.util.*;

public class PolicyLinter extends BaseLinter {

	Map<String, Set<String>> classToSub = new HashMap<>();
	Map<String, Set<String>> subToCat = new HashMap<>();
	Map<String, Set<String>> catToSubCat = new HashMap<>();

	public void addPoliciesToMaps(Policy policy){
		classToSub.put(policy.id, new HashSet<>());
		for(SubClass subclass : policy.subclasses){
			classToSub.get(policy.id).add(subclass.id);
			subToCat.put(subclass.id, new HashSet<>());
			for(Category category : subclass.categories){
				subToCat.get(subclass.id).add(category.id);
				catToSubCat.put(category.id, new HashSet<>());
				if(category.subcategories != null) {
					for (Field field : category.subcategories) {
						catToSubCat.get(category.id).add(field.id);
					}
				}
			}
		}
	}

	@Override
	public void loadTemplate(String path) throws IOException{
		ObjectMapper om = new ObjectMapper(new YAMLFactory());
		List<Policy> policies = om.readValue(new File(path), Policies.class).classes;

		for(Policy policy : policies){
			addPoliciesToMaps(policy);
		}
	}

	private boolean isMapValid(Map<String, Set<String>> map, String key, String val){
		return map.containsKey(key) && map.get(key).contains(val);
	}

	private boolean isValidFields(Map<String, Object> map){
		String pClass = (String) map.get("class");
		String subClass = (String) map.get("subclass");
		String category = (String) map.get("category");

		boolean isValidFields = isMapValid(classToSub, pClass, subClass) && isMapValid(subToCat, subClass, category);

		if("risk".equals(pClass) && isValidFields){
			String subCategory = (String) map.get("subcategory");
			isValidFields = isMapValid(catToSubCat, category, subCategory);
		}

		return isValidFields;
	}

	public void validatePolicies(String path) throws IOException {
		List<File> files = loadFilesFromPath(path, new String[]{"yaml"});
		for (File file : files) {
			if(file.getCanonicalPath().contains("policy-context")) {
				Map<String, Object> map = yaml.load(new FileInputStream(file));
				if(!isValidFields(map)){
					throw new ValidationException("Not a valid policy file: " + file.getName());
				}
			}
		}
	}

	@Override
	public void validate() throws IOException {
		String content = Thread.currentThread().getContextClassLoader().getResource("content").getFile();
		loadTemplate(content + "/schemas/policy-classification/policy-classification.yaml");
		validatePolicies(content + "/workflows");
	}
}
