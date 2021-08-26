package app.dassana.core.api.linter;

import app.dassana.core.api.ValidationException;
import app.dassana.core.contentmanager.ContentManager;
import com.google.gson.Gson;
import java.io.*;
import java.util.*;

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

		validateImages(content + "/schemas/vendors/icons");
		validateRequiredFields(content + "/workflows/vendors");
		validateFilter(content + "/workflows/csp");
	}

	private boolean hasValidFilter(Map<String, Object> data, File file) {
		if(file != null && !ignore.contains(file.getName())) return true;

		boolean isValid = true;
		if(ContentManager.POLICY_CONTEXT.equals((String) data.get("type"))) {
			List<Map<String, Object>> filters = (List<Map<String, Object>>) data.get("filters");
			for (int i = 0; i < filters.size() && isValid; i++) {
				Map<String, Object> filter = filters.get(i);
				isValid = filter.containsKey("vendor") ? template.contains(filter.get("vendor")) : false;
			}
		}
		return isValid;
	}

	public void validateFilterAPI(String json){
		Map<String, Object> data = gson.fromJson(json, Map.class);
		if(!hasValidFilter(data, null)){
			throw new ValidationException("Invalid filter setting in json");
		}
	}

	private void validateFilter(String path) throws FileNotFoundException {
		boolean containsVendor = true;
		List<File> files = loadFilesFromPath(path, new String[]{"yaml"});
		for (int i = 0; i < files.size() && containsVendor; i++) {
			File file = files.get(i);
			Map<String, Object> data = yaml.load(new FileInputStream(file));
			if(!hasValidFilter(data, file)){
				throw new ValidationException("Invalid filter setting in file: " + file.getName());
			}
		}
	}

	private boolean containsVendor(List<Map<String, Object>> outputs){
		boolean validVendor = false;
		Set<String> set = new HashSet<>();
		for (int i = 0; i < outputs.size(); i++) {
			Map<String, Object> output = outputs.get(i);
			String name = (String) output.get("name");
			if("vendorId".equals(name)){
				validVendor = template.contains((String) output.get("value"));
			}
			set.add(name);
		}
		return validVendor && required.containsAll(set);
	}

	public void validateRequiredFieldsAPI(String json){
		Map<String, Object> map = gson.fromJson(json, Map.class);
		List<Map<String, Object>> outputs = (List<Map<String, Object>>) map.get("output");
		if(!containsVendor(outputs)){
			throw new ValidationException("Is not valid normalizer for file");
		}
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
			List<Map<String, Object>> outputs = extractYamlArray(file, "output");
			if(!containsVendor(outputs)){
				throw new ValidationException("Is not valid normalizer for file: " + file.getName());
			}
		}
	}

	private void validateImages(String path) {
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
