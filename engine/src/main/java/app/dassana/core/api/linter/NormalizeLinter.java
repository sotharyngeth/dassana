package app.dassana.core.api.linter;

import app.dassana.core.api.ValidationException;
import app.dassana.core.contentmanager.ContentManager;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class NormalizeLinter extends BaseLinter{

	public final static String vendorListYamlPath = "/schemas/vendors/vendor-list.yaml";
	private final static String[] requiredFields = new String[]{"vendorId", "alertId", "vendorPolicy", "csp", "resourceContainer",
					"region", "service", "resourceType", "resourceId"};
	private Gson gson = new Gson();
	private Set<String> vendors = new HashSet<>();
	private Set<String> required = new HashSet<>();


	@Override
	public void init() throws IOException {
		List<Map<String, String>> dataArr = yaml.load((new FileInputStream(content + vendorListYamlPath)));
		for(Map<String,String> data : dataArr){
			vendors.add(data.get("id"));
		}
		required.addAll(Arrays.asList(requiredFields));
	}

	@Override
	public List<String> validate(String json) throws IOException {
		List<String> issues = new ArrayList<>();

		StatusMsg vendorIdStatus = validateVendorId(json);
		StatusMsg requiredStatus = validateRequiredFields(json);

		if(vendorIdStatus.isError()){
			issues.add(vendorIdStatus.toJson());
		}
		if(requiredStatus.isError()){
			issues.add(requiredStatus.toJson());
		}

		return issues;
	}

	@Override
	public void validate(Map<String, Object> data, String filename) throws IOException {
		validateRequiredFields(data, filename);
		validateVendorId(data, filename);
	}

	private StatusMsg validateVendorId(String json){
		Map<String, Object> data = gson.fromJson(json, Map.class);
		List<Map<String, Object>> outputs = (List<Map<String, Object>>) data.get(ContentManager.FIELDS.OUTPUT.getName());
		StatusMsg statusMsg = containsVendor(outputs);
		if(!statusMsg.isError()){
			statusMsg = validateNormVendor(data);
		}
		return statusMsg;
	}

	private StatusMsg validateNormVendor(Map<String, Object> data){
		String msg = null;
		String vendorId = (String) data.get(ContentManager.FIELDS.VENDOR_ID.getName());
		if(vendorId == null){
			msg = "Required1 field [vendor-id] is missing, " + helpText(vendors);
		}else if(!vendors.contains(data.get(ContentManager.FIELDS.VENDOR_ID.getName()))){
			msg = "Invalid vendor-id field [" + vendorId + "], " + helpText(vendors);
		}
		boolean isError = msg == null ? false : true;
		return new StatusMsg(isError, msg);
	}

	private StatusMsg validateRequiredFields(String json){
		Map<String, Object> data = gson.fromJson(json, Map.class);
		StatusMsg statusMsg = requiredFields(data);
		return statusMsg;
	}

	private void validateRequiredFields(Map<String, Object> data, String filename){
		StatusMsg statusMsg = requiredFields(data);
		if(statusMsg.isError()){
			throw new ValidationException(statusMsg.getMsg() + " in file: " + filename);
		}
	}

	private StatusMsg requiredFields(Map<String, Object> data){
		StatusMsg statusMsg = new StatusMsg(false);
		List<Map<String, Object>> outputs = (List<Map<String, Object>>) data.get(ContentManager.FIELDS.OUTPUT.getName());
		Set<String> names = new HashSet<>();
		if(outputs != null) {
			for (int i = 0; i < outputs.size(); i++) {
				Map<String, Object> output = outputs.get(i);
				names.add((String) output.get("name"));
			}
		}

		if(!names.containsAll(required)) {
			Set<String> requireCopy = new HashSet<>();
			requireCopy.addAll(required);
			requireCopy.removeAll(names);
			statusMsg = new StatusMsg(true, "Following required fields are missing: " + requireCopy);
		}

		return statusMsg;
	}

	private StatusMsg containsVendor(List<Map<String, Object>> outputs){
		StatusMsg statusMsg = new StatusMsg(false);
		if(outputs != null) {
			for (int i = 0; i < outputs.size() && !statusMsg.isError(); i++) {
				Map<String, Object> output = outputs.get(i);
				if ("vendorId".equals(output.get("name"))) {
					if (!vendors.contains((String) output.get("value"))) {
						statusMsg = new StatusMsg(true, "Invalid vendor id [" + output.get("value") + "]. " + helpText(vendors));
					}
				}
			}
		}

		return statusMsg;
	}

	private void validateVendorId(Map<String, Object> data, String filename){
		List<Map<String, Object>> outputs = (List<Map<String, Object>>) data.get(ContentManager.FIELDS.OUTPUT.getName());
		StatusMsg statusMsg = containsVendor(outputs);
		if(statusMsg.isError()){
			throw new ValidationException(statusMsg.getMsg() + " in file: " + filename);
		}
	}

	public void validateIcons() {
		List<File> files = loadFilesFromPath(content + "/schemas/vendors/icons", new String[]{"svg"});
		for (int i = 0; i < files.size(); i++) {
			File file = files.get(i);
			String name = file.getName().split(".svg")[0];
			if(!vendors.contains(name)){
				throw new ValidationException("Is missing image for file: " + file.getName() + ", " + helpText(vendors));
			}
		}
	}

}
