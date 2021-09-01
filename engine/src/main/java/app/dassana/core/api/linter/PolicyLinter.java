package app.dassana.core.api.linter;

import app.dassana.core.api.ValidationException;
import app.dassana.core.api.linter.pojo.*;
import app.dassana.core.contentmanager.ContentManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.gson.Gson;

import java.io.*;
import java.util.*;

public class PolicyLinter extends BaseLinter {

	private Map<String, Set<String>> classToSub = new HashMap<>();
	private Map<String, Set<String>> subToCat = new HashMap<>();
	private Map<String, Set<String>> catToSubCat = new HashMap<>();
	private Gson gson = new Gson();

	@Override
	public void loadTemplate(String path) throws IOException{
		ObjectMapper om = new ObjectMapper(new YAMLFactory());
		List<Policy> policies = om.readValue(new File(path), Policies.class).getClasses();
		for(Policy policy : policies){
			addPoliciesToMaps(policy);
		}
	}

	@Override
	public void validate() throws IOException {
		String content = Thread.currentThread().getContextClassLoader().getResource("content").getFile();
		loadTemplate(content + "/schemas/policy-classification/policy-classification.yaml");
		validatePolicies(content + "/workflows");
	}

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

	private boolean isMapValid(Map<String, Set<String>> map, String key, String val){
		return map.containsKey(key) && map.get(key).contains(val);
	}

	private String retrieveErrorField(String pClass, String subClass, String category, String subCategory){
		String errField = !classToSub.containsKey(pClass) ?  "invalid class: [" + pClass + "]" :
						!classToSub.get(pClass).contains(subClass) ? "invalid subclass: [" + subClass + "]" :
						!subToCat.get(subClass).contains(category) ? "invalid category: [" + category + "]" :
										                                     "invalid subcategory: [" + subCategory + "]";

		return errField;
	}

	private StatusMsg isValidFields(Map<String, Object> map){
		String pClass = (String) map.get("class");
		String subClass = (String) map.get("subclass");
		String category = (String) map.get("category");
		String subCategory = null;

		boolean isValidFields = isMapValid(classToSub, pClass, subClass) && isMapValid(subToCat, subClass, category);

		if("risk".equals(pClass) && isValidFields){
			subCategory = (String) map.get("subcategory");
			isValidFields = isMapValid(catToSubCat, category, subCategory);
		}

		StatusMsg statusMsg = new StatusMsg(false);

		if(!isValidFields){
			statusMsg.setError(true);
			statusMsg.setMsg(retrieveErrorField(pClass, subClass, category, subCategory));
		}

		return statusMsg;
	}

	public StatusMsg validatePoliciesAPI(String json){
		Map<String, Object> data = gson.fromJson(json, Map.class);
		StatusMsg statusMsg = isValidFields(data);
		return statusMsg;
	}

	private void validatePolicies(String path) throws IOException {
		List<File> files = loadFilesFromPath(path, new String[]{"yaml"});
		for (File file : files) {
			if(file.getCanonicalPath().contains(ContentManager.POLICY_CONTEXT)) {
				Map<String, Object> map = yaml.load(new FileInputStream(file));
				StatusMsg statusMsg = isValidFields(map);
				if(statusMsg.isError()){
					throw new ValidationException(statusMsg.getMsg() + " in file: " + file.getName());
				}
			}
		}
	}

}
