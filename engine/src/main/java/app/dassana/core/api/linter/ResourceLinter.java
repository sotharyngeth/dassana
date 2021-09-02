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

	public final static String hierarchyYamlPath = "/schemas/resource-hierarchy/resource-hierarchy.yaml";
	private Gson gson = new Gson();
	private Map<String, Set<String>> cspToService = new HashMap<>();
	private Map<String, Set<String>> serviceToResource = new HashMap<>();

	@Override
	public void loadTemplate(String path) throws IOException {
		ObjectMapper om = new ObjectMapper(new YAMLFactory());
		Csp csp = om.readValue(new File(path), Csp.class);

		for(Provider provider : csp.getProviders()){
			addResourcesToMaps(provider);
		}
	}

	@Override
	public void validate() throws IOException {
		String content = Thread.currentThread().getContextClassLoader().getResource("content").getFile();
		loadTemplate(content + hierarchyYamlPath);
		validateResources(content + "/workflows/csp");
	}

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

	private StatusMsg setErrorMessages(String csp, String service, String resource){
		String errField = !cspToService.containsKey(csp) ? "invalid csp: [" + csp + "]" :
						!cspToService.get(csp).contains(service) ? "invalid service: [" + service + "]" :
										"invalid resource: [" + resource + "]";
		return new StatusMsg(true, errField);
	}

	private StatusMsg retrieveMissingFields(Map<String, Object> map){
		String msg = null;

		if(!map.containsKey(ContentManager.FIELDS.CSP.getName())){
			msg = "missing csp field, suggested fields: " + cspToService.keySet();
		}else if(!map.containsKey(ContentManager.FIELDS.SERVICE.getName())){
			String csp = (String) map.get(ContentManager.FIELDS.CSP.getName());
			msg = "missing service field, suggested fields: " + cspToService.get(csp);
		}else{
			String service = (String) map.get(ContentManager.FIELDS.SERVICE.getName());
			msg = "missing resource-type field, suggested fields: " + serviceToResource.get(service);
		}

		return new StatusMsg(true, msg);
	}

	private StatusMsg isValidPolicy(Map<String, Object> map){
		StatusMsg statusMsg = new StatusMsg(false);

		if(map.containsKey(ContentManager.FIELDS.CSP.getName()) &&
						map.containsKey(ContentManager.FIELDS.SERVICE.getName()) &&
						map.containsKey(ContentManager.FIELDS.RESOURCE_TYPE.getName())){

			String csp = (String) map.get(ContentManager.FIELDS.CSP.getName()),
							service = (String) map.get(ContentManager.FIELDS.SERVICE.getName()),
							resource = (String) map.get(ContentManager.FIELDS.RESOURCE_TYPE.getName());

			boolean isValid = cspToService.containsKey(csp) && cspToService.get(csp).contains(service)
							&& serviceToResource.get(service).contains(resource);

			if(!isValid){
				statusMsg = setErrorMessages(csp, service, resource);
			}

		}else{
			statusMsg = retrieveMissingFields(map);
		}
		return statusMsg;
	}

	public StatusMsg validateResourcesAPI(String json){
		Map<String, Object> data = gson.fromJson(json, Map.class);
		StatusMsg statusMsg = isValidPolicy(data);
		return statusMsg;
	}

	private void validateResources(String path) throws FileNotFoundException {
		List<File> files = loadFilesFromPath(path, new String[]{"yaml"});
		for(File file : files){
			Map<String, Object> data = yaml.load(new FileInputStream(file));
			if(isResourceContext(data) || isPolicyContext(data)) {
				StatusMsg statusMsg = isValidPolicy(data);
				if (statusMsg.isError()) {
					throw new ValidationException(statusMsg.getMsg() + " in file: " + file.getPath());
				}
			}
		}
	}

}
