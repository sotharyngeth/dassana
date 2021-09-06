package app.dassana.core.api.linter;

import app.dassana.core.api.ValidationException;
import app.dassana.core.api.linter.pojo.*;
import app.dassana.core.contentmanager.ContentManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.gson.Gson;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

public class PolicyLinter extends ResourceLinter{

	public final static String vendorListYamlPath = "/schemas/vendors/vendor-list.yaml";
	public final static String classificationPath = "/schemas/policy-classification/policy-classification.yaml";
	private Map<String, Set<String>> classToSub = new HashMap<>();
	private Map<String, Set<String>> subToCat = new HashMap<>();
	private Map<String, Set<String>> catToSubCat = new HashMap<>();
	private Set<String> vendors = new HashSet<>();
	private Gson gson = new Gson();


	@Override
	public List<String> validate(String json) throws IOException {
		List<String> issues = new ArrayList<>();

		issues.addAll(super.validate(json));

		StatusMsg policyStatus = validatePolicyHiearchy(json);
		StatusMsg filterStatus = validateFilter(json);

		if(policyStatus.isError()){
			issues.add(policyStatus.toJson());
		}
		if(filterStatus.isError()){
			issues.add(filterStatus.toJson());
		}

		return issues;
	}

	@Override
	public void validate(Map<String, Object> data, String filename) throws IOException {
		super.validate(data, filename);
		validateFilter(data, filename);
		validatePolicyHiearchy(data, filename);
	}

	@Override
	public void init() throws IOException {
		super.init();
		loadVendors();
		loadPolicies();
	}

	private void loadPolicies() throws IOException {
		ObjectMapper om = new ObjectMapper(new YAMLFactory());
		List<Policy> policies = om.readValue(new File(content + classificationPath), Policies.class).getClasses();
		for(Policy policy : policies){
			addPoliciesToMaps(policy);
		}
	}

	private void loadVendors() throws FileNotFoundException {
		List<Map<String, String>> dataArr = yaml.load((new FileInputStream(content + vendorListYamlPath)));
		for(Map<String,String> data : dataArr){
			vendors.add(data.get("id"));
		}
	}

	private void validateFilter(Map<String, Object> data, String filename){
		StatusMsg statusMsg = isValidFields(data);
		if(statusMsg.isError()){
			throw new ValidationException(statusMsg.getMsg() + " in file: " + filename);
		}
	}

	private StatusMsg validateFilter(String json){
		Map<String, Object> data = gson.fromJson(json, Map.class);
		StatusMsg statusMsg = hasValidFilter(data);
		return statusMsg;
	}

	private StatusMsg hasValidFilter(Map<String, Object> data) {
		StatusMsg statusMsg = new StatusMsg(false);

		boolean isValid = true;
		if(containsFilters(data)) {
			List<Map<String, Object>> filters = (List<Map<String, Object>>) data.get("filters");
			for (int i = 0; i < filters.size() && isValid; i++) {
				Map<String, Object> filter = filters.get(i);
				isValid = filter.containsKey("vendor") ? vendors.contains(filter.get("vendor")) : false;
				if(!isValid){
					statusMsg.setError(true);
					String vendorErr = filter.get("vendor") == null ? "Missing vendor id" :
									"Invalid vendor id [ " + filter.get("vendor") + "]";
					statusMsg.setMsg(vendorErr + " in filters array, " + helpText(vendors));
				}
			}
		}
		return statusMsg;
	}

	private boolean containsFilters(Map<String, Object> data){
		boolean containsFilter = data.containsKey("filters");
		List<Object> filters = (List<Object>) data.get("filters");
		return containsFilter && filters != null && filters.size() > 0;
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

	private void validatePolicyHiearchy(Map<String, Object> data, String filename) {
		StatusMsg statusMsg = isValidFields(data);
		if(statusMsg.isError()){
			throw new ValidationException(statusMsg.getMsg() + " in file: " + filename);
		}
	}

	private StatusMsg validatePolicyHiearchy(String json){
		Map<String, Object> data = gson.fromJson(json, Map.class);
		StatusMsg statusMsg = isValidFields(data);
		return statusMsg;
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
			if(isRisk){
				isValid = isValid && isMapValid(catToSubCat, category, subCategory);
			}
			if(!isValid){
				statusMsg = retrieveErrorField(alertClass, subClass, category, subCategory, isRisk);
			}
		}

		if(!statusMsg.isError()){
			statusMsg = validateIncident(category, subCategory);
		}

		return statusMsg;
	}

	private StatusMsg retrieveErrorField(String alertClass, String subClass, String category, String subCategory, boolean isRisk){
		String msg = "";

		if(!classToSub.containsKey(alertClass)){
			msg = "invalid class: [" + alertClass + "], " + helpText(classToSub);
		}else if(!classToSub.get(alertClass).contains(subClass)){
			msg = "invalid subclass: [" + subClass + "], " + helpText(classToSub, alertClass);
		}else if(!subToCat.get(subClass).contains(category)){
			msg = "invalid category: [" + category + "], " + helpText(subToCat, subClass);
		}else if(isRisk){
			msg = "invalid subcategory: [" + subCategory + "], " + helpText(catToSubCat, category);
		}

		return new StatusMsg(true, msg);
	}

	private StatusMsg validateIncident(String category, String subCategory){
		StatusMsg statusMsg = new StatusMsg(false);
		if(subCategory != null && !catToSubCat.get(category).contains(subCategory)){
			statusMsg.setError(true);
			statusMsg.setMsg("invalid subcategory: [" + subCategory + "], " + helpText(catToSubCat, category));
		}
		return statusMsg;
	}

	private boolean isMapValid(Map<String, Set<String>> map, String key, String val){
		return map.containsKey(key) && map.get(key).contains(val);
	}

	private StatusMsg retrieveMissingField(String alertClass, String subClass, String category, boolean isRisk){
		String msg = null;

		if(alertClass == null){
			msg = "missing class field, " + helpText(classToSub);
		}else if(subClass == null){
			msg = "missing subclass field, " + helpText(classToSub, alertClass);
		}else if(category == null){
			msg = "missing category field, " + helpText(subToCat, subClass);
		}else if(isRisk){
			msg = "missing subcategory field, " + helpText(catToSubCat, category);
		}

		return new StatusMsg(true, msg);
	}

}
