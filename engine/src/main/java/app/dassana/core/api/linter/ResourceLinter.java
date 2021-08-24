package app.dassana.core.api.linter;

import app.dassana.core.api.ValidationException;
import app.dassana.core.api.linter.pojo.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

public class ResourceLinter extends BaseLinter{

	Map<String, Set<String>> cspToService = new HashMap<>();
	Map<String, Set<String>> serviceToResource = new HashMap<>();

	private void addResourcesToMaps(Provider provider){
		cspToService.put(provider.id, new HashSet<>());
		for(Service service : provider.services){
			cspToService.get(provider.id).add(service.id);
			serviceToResource.put(service.id, new HashSet<>());
			for(Field resource : service.resources){
				serviceToResource.get(service.id).add(resource.id);
			}
		}
	}

	@Override
	public void loadTemplate(String path) throws IOException {
		ObjectMapper om = new ObjectMapper(new YAMLFactory());
		Csp csp = om.readValue(new File(path), Csp.class);

		for(Provider provider : csp.providers){
			addResourcesToMaps(provider);
		}
	}

	private boolean isValidPolicy(Map<String, Object> map){
		boolean validPolicy = "general-context".equals(map.get("type"));

		if(map.containsKey("csp") && map.containsKey("service") && map.containsKey("resource-type") && !validPolicy){

			String csp = (String) map.get("csp"), service = (String) map.get("service"),
							resource = (String) map.get("resource-type");

			validPolicy = cspToService.containsKey(csp) && cspToService.get(csp).contains(service)
							&& serviceToResource.get(service).contains(resource);
		}
		return validPolicy;
	}

	public void validateResources(String path) throws FileNotFoundException {
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
