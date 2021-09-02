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

	public final static String classificationPath = "/schemas/policy-classification/policy-classification.yaml";
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
		loadTemplate(content + classificationPath);
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

	private StatusMsg retrieveMissingField(String alertClass, String subClass, String category, boolean isRisk){
		String msg = null;

		if(alertClass == null){
			//msg = "missing class field, available fields: " + classToSub.keySet();
			msg = "missing class field, " + getAvailableFields(classToSub);
		}else if(subClass == null){
			//msg = "missing subclass field, available fields: " + classToSub.get(alertClass);
			msg = "missing subclass field, " + getAvailableFields(classToSub, alertClass);
		}else if(category == null){
			//msg = "missing category field, available fields: " + subToCat.get(subClass);
			msg = "missing category field, " + getAvailableFields(subToCat, subClass);
		}else if(isRisk){
			//msg = "missing subcategory field, available fields: " + catToSubCat.get(category);
			msg = "missing subcategory field, " + getAvailableFields(catToSubCat, category);
		}

		return new StatusMsg(true, msg);
	}

	private StatusMsg retrieveErrorField(String alertClass, String subClass, String category, String subCategory, boolean isRisk){
		String msg = "";

		if(!classToSub.containsKey(alertClass)){
			msg = "invalid class: [" + alertClass + "], " + getAvailableFields(classToSub);
		}else if(!classToSub.get(alertClass).contains(subClass)){
			msg = "invalid subclass: [" + subClass + "], " + getAvailableFields(classToSub, alertClass);
		}else if(!subToCat.get(subClass).contains(category)){
			msg = "invalid category: [" + category + "], " + getAvailableFields(subToCat, subClass);
		}else if(isRisk){
			msg = "invalid subcategory: [" + subCategory + "], " + getAvailableFields(catToSubCat, category);
		}

		return new StatusMsg(true, msg);
	}

	private StatusMsg isValidFields(Map<String, Object> map){
		String alertClass = (String) map.get(ContentManager.FIELDS.CLASS.getName());
		String subClass = (String) map.get(ContentManager.FIELDS.SUB_CLASS.getName());
		String category = (String) map.get(ContentManager.FIELDS.CATEGORY.getName());
		String subCategory = (String) map.get(ContentManager.FIELDS.SUB_CATEGORY.getName());;

		StatusMsg statusMsg = new StatusMsg(false);

		boolean isRisk  = "risk".equals(alertClass);

		boolean hasBaseFields = alertClass != null && subClass != null && category != null;

		if(isRisk){
			hasBaseFields = hasBaseFields && (isRisk && subCategory != null);
		}

		//if missing required fields for risk or incident
		if(!hasBaseFields){
			statusMsg = retrieveMissingField(alertClass, subClass, category, isRisk);
		}else{
			boolean isValid = isMapValid(classToSub, alertClass, subClass) && isMapValid(subToCat, subClass, category);
			//if risk make sure subcategory is valid
			if(isRisk){
				isValid = isValid && isMapValid(catToSubCat, category, subCategory);
			}

			if(!isValid){
				statusMsg = retrieveErrorField(alertClass, subClass, category, subCategory, isRisk);
			}
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
			Map<String, Object> map = yaml.load(new FileInputStream(file));
			if(isPolicyContext(map)) {
				StatusMsg statusMsg = isValidFields(map);
				if (statusMsg.isError()) {
					throw new ValidationException(statusMsg.getMsg() + " in file: " + file.getName());
				}
			}
		}
	}

}
