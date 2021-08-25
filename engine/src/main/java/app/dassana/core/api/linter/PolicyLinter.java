package app.dassana.core.api.linter;

import app.dassana.core.api.ValidationException;
import app.dassana.core.api.linter.pojo.*;
import app.dassana.core.contentmanager.ContentManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.*;
import java.util.*;

public class PolicyLinter extends BaseLinter {

	Map<String, Set<String>> classToSub = new HashMap<>();
	Map<String, Set<String>> subToCat = new HashMap<>();
	Map<String, Set<String>> catToSubCat = new HashMap<>();

	public void addPoliciesToMaps(Policy policy){
		classToSub.put(policy.getId(), new HashSet<>());
		for(SubClass subclass : policy.getSubclasses()){
			classToSub.get(policy.getId()).add(subclass.getId());
			subToCat.put(subclass.getId(), new HashSet<>());
			for(Category category : subclass.getCategories()){
				subToCat.get(subclass.getId()).add(category.getId());
				catToSubCat.put(category.getId(), new HashSet<>());
				if(category.getSubcategories() != null) {
					for (Field field : category.getSubcategories()) {
						catToSubCat.get(category.getId()).add(field.getId());
					}
				}
			}
		}
	}

	@Override
	public void loadTemplate(String path) throws IOException{
		ObjectMapper om = new ObjectMapper(new YAMLFactory());
		List<Policy> policies = om.readValue(new File(path), Policies.class).getClasses();

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
			if(file.getCanonicalPath().contains(ContentManager.POLICY_CONTEXT)) {
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
