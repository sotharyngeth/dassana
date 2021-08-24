package app.dassana.core.api.linter;

import app.dassana.core.api.ValidationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

public class VendorLinter extends BaseLinter {

	Set<String> template = new HashSet<>();

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
		loadTemplate(content + "/schemas/vendors/vendor-list.yaml");

		validateImages(content + "/schemas/vendors/icons");
		validateNorm(content + "/workflows/vendors");
		validateFilter(content + "/workflows/csp");
		validateKeysExist(content + "/workflows/vendors");
	}


	private boolean hasValidFilter(File file) throws FileNotFoundException {
		boolean isValid = true;
		Map<String, Object> data = yaml.load(new FileInputStream(file));
		if("policy-context".equals((String) data.get("type"))) {
			List<Map<String, Object>> filters = (List<Map<String, Object>>) data.get("filters");
			for (int i = 0; i < filters.size() && isValid; i++) {
				Map<String, Object> filter = filters.get(i);
				isValid = filter.containsKey("vendor") ? template.contains(filter.get("vendor")) : false;
			}
		}
		return isValid;
	}

	//valid filter check
	public void validateFilter(String path) throws FileNotFoundException {
		boolean containsVendor = true;
		List<File> files = loadFilesFromPath(path, new String[]{"yaml"});
		for (int i = 0; i < files.size() && containsVendor; i++) {
			File file = files.get(i);
			if(!hasValidFilter(file)){
				throw new ValidationException("Invalid filter setting in file: " + file.getName());
			}
		}
	}

	private boolean containsVendor(File file) throws FileNotFoundException {
		boolean isValidVendor = false;
		List<Map<String, Object>> outputs = extractYamlArray(file, "output");
		for (int i = 0; i < outputs.size() && !isValidVendor; i++) {
			Map<String, Object> output = outputs.get(i);
			isValidVendor = "vendorId".equals(output.get("name"));
		}
		return isValidVendor;
	}

	//valid normalizer check
	public void validateNorm(String path) throws FileNotFoundException {
		List<File> files = loadFilesFromPath(path, new String[]{"yaml"});
		for (int i = 0; i < files.size(); i++) {
			File file = files.get(i);
			if(!containsVendor(file)){
				throw new ValidationException("Is not valid normalizer for file: " + file.getName());
			}
		}
	}

	//valid images check
	public void validateImages(String path) {
		List<File> files = loadFilesFromPath(path, new String[]{"svg"});
		for (int i = 0; i < files.size(); i++) {
			File file = files.get(i);
			String name = file.getName().split(".svg")[0];
			if(!template.contains(name)){
				throw new ValidationException("Is missing image for file: " + file.getName());
			}
		}
	}

	public void validateKeysExist(String path) throws IOException {
		List<File> files = loadFilesFromPath(path, new String[]{"yaml"});
		for(File file : files){
			boolean vendorIdExists = false;
			List<Map<String, Object>> outputs = extractYamlArray(file, "output");
			for(Map<String, Object> output : outputs){
				vendorIdExists = vendorIdExists || output.containsValue("vendorId");
			}
			if(!vendorIdExists){
				throw new ValidationException("Key missing from file: " + file.getName());
			}
		}
	}

}
