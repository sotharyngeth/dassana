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
		loadTemplate(content + "/schemas/resource-hierarchy/resource-hierarchy.yaml");
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
		StatusMsg statusMsg = new StatusMsg(true);
		String errField = !cspToService.containsKey(csp) ? "invalid csp: [" + csp + "]" :
						!cspToService.get(csp).contains(service) ? "invalid service: [" + service + "]" :
										"invalid resource: [" + resource + "]";
		statusMsg.setMsg(errField);
		return statusMsg;
	}

	private StatusMsg isValidPolicy(Map<String, Object> map){
		StatusMsg statusMsg = new StatusMsg(false);
		boolean isGeneral = ContentManager.GENERAL_CONTEXT.equals(map.get("type"));

		if(!isGeneral && map.containsKey("csp") && map.containsKey("service") && map.containsKey("resource-type")){
			String csp = (String) map.get("csp"), service = (String) map.get("service"),
							resource = (String) map.get("resource-type");

			boolean validPolicy = cspToService.containsKey(csp) && cspToService.get(csp).contains(service)
							&& serviceToResource.get(service).contains(resource);

			if(!validPolicy){
				statusMsg = setErrorMessages(csp, service, resource);
			}
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
			StatusMsg statusMsg = isValidPolicy(data);
			if(statusMsg.isError()){
				throw new ValidationException(statusMsg.getMsg() + " in file: " + file.getPath());
			}
		}
	}

}
