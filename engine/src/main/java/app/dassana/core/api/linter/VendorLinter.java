package app.dassana.core.api.linter;

import app.dassana.core.api.ValidationException;
import app.dassana.core.contentmanager.ContentManager;
import com.google.gson.Gson;
import java.io.*;
import java.util.*;

public class VendorLinter extends BaseLinter {

	public final static String vendorListYamlPath = "/schemas/vendors/vendor-list.yaml";
	private Set<String> template = new HashSet<>();
	private Gson gson = new Gson();

	@Override
	public void loadTemplate(String path) throws FileNotFoundException {
		List<Map<String, String>> dataArr = yaml.load((new FileInputStream(path)));
		for(Map<String,String> data : dataArr){
			template.add(data.get("id"));
		}
	}

	@Override
	public void validate() throws IOException {
		String content = Thread.currentThread().getContextClassLoader().getResource("content").getFile();
		loadTemplate(content + vendorListYamlPath);

		validateIcons(content + "/schemas/vendors/icons");
		validateVendorId(content + "/workflows");
		validateFilter(content + "/workflows/csp");
	}

	private boolean containsFilters(Map<String, Object> data){
		boolean containsFilter = data.containsKey("filters");
		List<Object> filters = (List<Object>) data.get("filters");
		return containsFilter && filters != null && filters.size() > 0;
	}

	private StatusMsg hasValidFilter(Map<String, Object> data) {
		StatusMsg statusMsg = new StatusMsg(false);

		boolean isValid = true;
		if(containsFilters(data)) {
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
			Map<String, Object> data = yaml.load(new FileInputStream(file));
			if(isPolicyContext(data)) {
				StatusMsg statusMsg = hasValidFilter(data);
				if (statusMsg.isError()) {
					throw new ValidationException(statusMsg.getMsg() + " in file: " + file.getName());
				}
			}
		}
	}

	private StatusMsg containsVendor(List<Map<String, Object>> outputs){
		StatusMsg statusMsg = new StatusMsg(false);
		if(outputs != null) {
			for (int i = 0; i < outputs.size() && !statusMsg.isError(); i++) {
				Map<String, Object> output = outputs.get(i);
				String name = (String) output.get("name");
				if ("vendorId".equals(name)) {
					if (!template.contains((String) output.get("value"))) {
						statusMsg.setMsg("Invalid vendor id [" + output.get("value") + "]");
						statusMsg.setError(true);
					}
				}
			}
		}

		return statusMsg;
	}

	public StatusMsg validateVendorIdAPI(String json){
		Map<String, Object> map = gson.fromJson(json, Map.class);
		List<Map<String, Object>> outputs = (List<Map<String, Object>>) map.get(ContentManager.FIELDS.OUTPUT.getName());
		StatusMsg statusMsg = containsVendor(outputs);
		return statusMsg;
	}

	/**
	 * Runs validation normalizer check on all files
	 * @param path
	 * @throws FileNotFoundException
	 */
	private void validateVendorId(String path) throws FileNotFoundException {
		List<File> files = loadFilesFromPath(path, new String[]{"yaml"});
		for (int i = 0; i < files.size(); i++) {
			File file = files.get(i);
			Map<String, Object> data = yaml.load(new FileInputStream(file));
			if(data.containsKey(ContentManager.FIELDS.OUTPUT.getName())) {
				List<Map<String, Object>> outputs = (List<Map<String, Object>>) data.get(ContentManager.FIELDS.OUTPUT.getName());
				StatusMsg errorField = containsVendor(outputs);
				if (errorField.isError()) {
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
