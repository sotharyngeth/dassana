package app.dassana.core.api.linter;

import app.dassana.core.api.ValidationException;
import app.dassana.core.api.linter.pojo.Csp;
import app.dassana.core.api.linter.pojo.Field;
import app.dassana.core.api.linter.pojo.Provider;
import app.dassana.core.api.linter.pojo.Service;
import app.dassana.core.contentmanager.ContentManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.gson.Gson;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class ResourceLinter extends GeneralLinter{

	public final static String hierarchyYamlPath = "/schemas/resource-hierarchy/resource-hierarchy.yaml";
	private Gson gson = new Gson();
	private Map<String, Set<String>> cspToService = new HashMap<>();
	private Map<String, Set<String>> serviceToResource = new HashMap<>();

	@Override
	public List<String> validate(String json) throws IOException {
		List<String> issues = new ArrayList<>();

		StatusMsg stepStatus = validateSteps(json);
		if(stepStatus.isError()) {
			issues.add(stepStatus.toJson());
		}

		StatusMsg resourceStatus = validateResourceHierarchy(json);

		if(resourceStatus.isError()){
			issues.add(resourceStatus.toJson());
		}

		return issues;
	}

	@Override
	public void validate(Map<String, Object> data, String filename) throws IOException {
		super.validate(data, filename);
		validateResourceHierarchy(data, filename);
	}

	@Override
	public void init() throws IOException {
		super.init();
		ObjectMapper om = new ObjectMapper(new YAMLFactory());
		Csp csp = om.readValue(new File(content + hierarchyYamlPath), Csp.class);

		for(Provider provider : csp.getProviders()){
			addResourcesToMaps(provider);
		}
	}

	private void validateResourceHierarchy(Map<String, Object> data, String filename) {
		StatusMsg statusMsg = isValidPolicy(data);
		if(statusMsg.isError()){
			throw new ValidationException(statusMsg.getMsg() + " in file: " + filename);
		}
	}

	private StatusMsg validateResourceHierarchy(String json){
		Map<String, Object> data = gson.fromJson(json, Map.class);
		StatusMsg statusMsg = isValidPolicy(data);
		return statusMsg;
	}

	private StatusMsg isValidPolicy(Map<String, Object> map){
		StatusMsg statusMsg = new StatusMsg(false);

		String csp = (String) map.get(ContentManager.FIELDS.CSP.getName()),
						service = (String) map.get(ContentManager.FIELDS.SERVICE.getName()),
						resource = (String) map.get(ContentManager.FIELDS.RESOURCE_TYPE.getName());

		if(csp == null || service == null || resource == null){
			statusMsg = retrieveMissingFields(map);
		}else{
			boolean isValid = cspToService.containsKey(csp) && cspToService.get(csp).contains(service)
							&& serviceToResource.get(service).contains(resource);

			if(!isValid){
				statusMsg = setErrorMessages(csp, service, resource);
			}
		}

		return statusMsg;
	}

	private StatusMsg setErrorMessages(String csp, String service, String resource){
		String errField = !cspToService.containsKey(csp) ? "invalid csp: [" + csp + "], " +  helpText(cspToService):
						!cspToService.get(csp).contains(service) ? "invalid service: [" + service + "], " + helpText(cspToService, csp):
										"invalid resource: [" + resource + "], " + helpText(serviceToResource, service);
		return new StatusMsg(true, errField);
	}

	private StatusMsg retrieveMissingFields(Map<String, Object> map){
		String msg = null;

		if(!map.containsKey(ContentManager.FIELDS.CSP.getName())){
			msg = "missing csp field, " + helpText(cspToService);
		}else if(!map.containsKey(ContentManager.FIELDS.SERVICE.getName())){
			String csp = (String) map.get(ContentManager.FIELDS.CSP.getName());
			msg = "missing service field, " + helpText(cspToService, csp);
		}else{
			String service = (String) map.get(ContentManager.FIELDS.SERVICE.getName());
			msg = "missing resource field, " + helpText(serviceToResource, service);
		}

		return new StatusMsg(true, msg);
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

}
