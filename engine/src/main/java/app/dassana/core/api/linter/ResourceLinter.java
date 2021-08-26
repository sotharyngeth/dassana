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

public class ResourceLinter extends BaseLinter{

	private Gson gson = new Gson();
	private Map<String, Set<String>> cspToService = new HashMap<>();
	private Map<String, Set<String>> serviceToResource = new HashMap<>();

	private void addResourcesToMaps(Provider provider){
		cspToService.put(provider.getId(), new HashSet<>());
		for(Service service : provider.getServices()){
			cspToService.get(provider.getId()).add(service.getId());
			serviceToResource.put(service.getId(), new HashSet<>());
			for(Field resource : service.getResources()){
				serviceToResource.get(service.getId()).add(resource.getId());
			}
		}
	}

	@Override
	public void loadTemplate(String path) throws IOException {
		ObjectMapper om = new ObjectMapper(new YAMLFactory());
		Csp csp = om.readValue(new File(path), Csp.class);

		for(Provider provider : csp.getProviders()){
			addResourcesToMaps(provider);
		}
	}

	private boolean isValidPolicy(Map<String, Object> map){
		boolean validPolicy = ContentManager.GENERAL_CONTEXT.equals(map.get("type"));

		if(map.containsKey("csp") && map.containsKey("service") && map.containsKey("resource-type") && !validPolicy){

			String csp = (String) map.get("csp"), service = (String) map.get("service"),
							resource = (String) map.get("resource-type");

			validPolicy = cspToService.containsKey(csp) && cspToService.get(csp).contains(service)
							&& serviceToResource.get(service).contains(resource);
		}
		return validPolicy;
	}

	public void validateResourcesAPI(String json){
		Map<String, Object> data = gson.fromJson(json, Map.class);
		if(!isValidPolicy(data)){
			throw new ValidationException("Invalid resource mapping in json");
		}
	}

	private void validateResources(String path) throws FileNotFoundException {
		List<File> files = loadFilesFromPath(path, new String[]{"yaml"});
		for(File file : files){
			Map<String, Object> map = yaml.load(new FileInputStream(file));
			if(!isValidPolicy(map)){
				throw new ValidationException("Invalid resource mapping in file, " + file.getName());
			}
		}
	}

	@Override
	public void validate() throws IOException {
		String content = Thread.currentThread().getContextClassLoader().getResource("content").getFile();
		loadTemplate(content + "/schemas/resource-hierarchy/resource-hierarchy.yaml");
		validateResources(content + "/workflows/csp");
	}
}
