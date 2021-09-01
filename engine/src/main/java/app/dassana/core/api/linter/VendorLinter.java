package app.dassana.core.api.linter;

import app.dassana.core.api.ValidationException;
import app.dassana.core.contentmanager.ContentManager;
import com.google.gson.Gson;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class VendorLinter extends BaseLinter {

	private Set<String> template = new HashSet<>(), ignore, required;
	private Gson gson = new Gson();

	public void buildIgnoreList(){
		String[] ignoreList = new String[]{"config-recording-not-enabled.yaml", "user-has-console-and-access-keys.yaml",
		"iam-role-not-enabled-for-ec2.yaml", "vpc-route-table-is-overly-permissive.yaml"};

		String[] requireList = new String[]{"alertId", "canonicalId", "vendorId", "vendorPolicy", "csp",
						"resourceContainer", "region", "service", "resourceType", "resourceId"};

		ignore   = Set.of(ignoreList);
		required = Set.of(requireList);
	}

	@Override
	public void loadTemplate(String path) throws FileNotFoundException {
		List<Map<String, String>> dataArr = yaml.load((new FileInputStream(path)));
		for(Map<String,String> data : dataArr){
			template.add(data.get("id"));
		}
		buildIgnoreList();
	}

	@Override
	public void validate() throws IOException {
		String content = Thread.currentThread().getContextClassLoader().getResource("content").getFile();
		loadTemplate(content + "/schemas/vendors/vendor-list.yaml");

		validateIcons(content + "/schemas/vendors/icons");
		validateRequiredFields(content + "/workflows/vendors");
		validateFilter(content + "/workflows/csp");
	}

	private StatusMsg hasValidFilter(Map<String, Object> data) {
		StatusMsg statusMsg = new StatusMsg(false);

		boolean isValid = true;
		if(ContentManager.POLICY_CONTEXT.equals(data.get("type"))) {
			List<Map<String, Object>> filters = (List<Map<String, Object>>) data.get("filters");
			for (int i = 0; i < filters.size() && isValid; i++) {
				Map<String, Object> filter = filters.get(i);
				isValid = filter.containsKey("vendor") ? template.contains(filter.get("vendor")) : false;
				if(!isValid){
					statusMsg.setError(true);
					statusMsg.setMsg("Invalid vendor id [" + filter.get("vendor") + "] in filters array");
				}
			}
		}
		return statusMsg;
	}

	public StatusMsg validateFilterAPI(String json){
		Map<String, Object> data = gson.fromJson(json, Map.class);
		StatusMsg statusMsg = hasValidFilter(data);
		return statusMsg;
	}

	private void validateFilter(String path) throws FileNotFoundException {
		boolean containsVendor = true;
		List<File> files = loadFilesFromPath(path, new String[]{"yaml"});
		for (int i = 0; i < files.size() && containsVendor; i++) {
			File file = files.get(i);
			if(!ignore.contains(file.getName())) {
				Map<String, Object> data = yaml.load(new FileInputStream(file));
				StatusMsg statusMsg = hasValidFilter(data);
				if (statusMsg.isError()) {
					throw new ValidationException(statusMsg.getMsg() + " in file: " + file.getName());
				}
			}
		}
	}

	private StatusMsg containsVendor(List<Map<String, Object>> outputs){
		boolean validVendor = true;
		StatusMsg statusMsg = new StatusMsg(false);
		Set<String> set = new HashSet<>();
		for (int i = 0; i < outputs.size() && validVendor; i++) {
			Map<String, Object> output = outputs.get(i);
			String name = (String) output.get("name");
			if("vendorId".equals(name)){
				validVendor = template.contains((String) output.get("value"));
				if(!validVendor){
					statusMsg.setMsg("Invalid vendor id [" + output.get("value") + "]");
					statusMsg.setError(true);
				}
			}
			set.add(name);
		}

		Set<String> reqCopy = required.stream().collect(Collectors.toSet());
		reqCopy.removeAll(set);

		if(validVendor && !reqCopy.isEmpty()){
			statusMsg.setMsg("Missing required fields: " + reqCopy);
			statusMsg.setError(true);
		}

		return statusMsg;
	}

	public StatusMsg validateRequiredFieldsAPI(String json){
		Map<String, Object> map = gson.fromJson(json, Map.class);
		List<Map<String, Object>> outputs = (List<Map<String, Object>>) map.get("output");
		StatusMsg statusMsg = containsVendor(outputs);
		return statusMsg;
	}

	/**
	 * Runs validation normalizer check on all files
	 * @param path
	 * @throws FileNotFoundException
	 */
	private void validateRequiredFields(String path) throws FileNotFoundException {
		List<File> files = loadFilesFromPath(path, new String[]{"yaml"});
		for (int i = 0; i < files.size(); i++) {
			File file = files.get(i);
			if(!ignore.contains(file.getName())){
				List<Map<String, Object>> outputs = extractYamlArray(file, "output");
				StatusMsg errorField = containsVendor(outputs);
				if(errorField.isError()) {
					throw new ValidationException("Is not valid normalizer, incorrect field: " + errorField.getMsg() +
									" for file: " + file.getName());
				}
			}
		}
	}

	private void validateIcons(String path) {
		List<File> files = loadFilesFromPath(path, new String[]{"svg"});
		for (int i = 0; i < files.size(); i++) {
			File file = files.get(i);
			String name = file.getName().split(".svg")[0];
			if(!template.contains(name)){
				throw new ValidationException("Is missing image for file: " + file.getName());
			}
		}
	}

}
