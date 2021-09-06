package app.dassana.core.api.linter;

import app.dassana.core.api.ValidationException;
import app.dassana.core.api.linter.pojo.Csp;
import app.dassana.core.api.linter.pojo.Provider;
import app.dassana.core.contentmanager.ContentManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.gson.Gson;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

public class GeneralLinter extends BaseLinter{

	private Gson gson = new Gson();
	private Set<String> actions = new HashSet<>();
	private Set<String> csps = new HashSet<>();
	public final static String actionTemplatePath = "/actions";

	@Override
	public void init() throws IOException {
		loadActions();
		loadCsps();
	}

	@Override
	public List<String> validate(String json) throws IOException {
		List<String> issues = new ArrayList<>();

		StatusMsg cspStatus    = validateCsp(json);
		StatusMsg actionStatus = validateSteps(json);

		if(cspStatus.isError()){
			issues.add(cspStatus.toJson());
		}
		if(actionStatus.isError()){
			issues.add(actionStatus.toJson());
		}

		return getIssuesAsJson(issues);
	}

	@Override
	public void validate(Map<String, Object> data, String filename) throws IOException {
		validateCsp(data, filename);
		validateSteps(data, filename);
	}

	private void validateCsp(Map<String, Object> data, String filename){
		StatusMsg statusMsg = validateNormVendor(ContentManager.FIELDS.CSP, data, csps);
		if(statusMsg.isError()){
			throw new ValidationException(statusMsg.getMsg() + " in file: " + filename);
		}
	}

	private void loadCsps() throws IOException {
		ObjectMapper om = new ObjectMapper(new YAMLFactory());
		Csp csp = om.readValue(new File(content + "/schemas/resource-hierarchy/resource-hierarchy.yaml"), Csp.class);

		for(Provider provider : csp.getProviders()){
			String cspField = provider.getId();
			csps.add(cspField);
		}
	}

	private void loadActions() throws FileNotFoundException {
		List<File> files = loadFilesFromPath(content + actionTemplatePath, new String[]{"yaml"});
		for(File file : files){
			Map<String, Object> data = yaml.load(new FileInputStream(file));
			actions.add((String)data.get("id"));
		}
	}

	private StatusMsg validateCsp(String json) {
		Map<String, Object> data = gson.fromJson(json, Map.class);
		StatusMsg statusMsg = validateNormVendor(ContentManager.FIELDS.CSP, data, csps);
		return statusMsg;
	}

	protected void validateSteps(Map<String, Object> data, String filename) {
		StatusMsg statusMsg = validateYaml(data);
		if(statusMsg.isError()){
			throw new ValidationException(statusMsg.getMsg() + " in file: " + filename);
		}
	}

	protected StatusMsg validateSteps(String json){
		Map<String, Object> data = gson.fromJson(json, Map.class);
		StatusMsg statusMsg = validateYaml(data);
		return statusMsg;
	}

	private StatusMsg validateYaml(Map<String, Object> data) {
		StatusMsg statusMsg = new StatusMsg(false);
		if(data.containsKey(ContentManager.FIELDS.STEPS.getName())){
			List<Map<String, Object>> steps = (List<Map<String, Object>>) data.get(ContentManager.FIELDS.STEPS.getName());
			statusMsg = checkStepsForErrors(steps);
		}
		return statusMsg;
	}

	private StatusMsg checkStepsForErrors(List<Map<String, Object>> steps){
		boolean isError = false;
		String errorMsg = null;
		for(int i = 0; i < steps.size() && !isError; i++){
			Map<String, Object> step = steps.get(i);
			if(!actions.contains(step.get(ContentManager.FIELDS.USES.getName()))){
				isError  = true;
				errorMsg = "Invalid uses field: [" +  step.get(ContentManager.FIELDS.USES.getName()) + "], " + helpText(actions);
			}
		}
		return new StatusMsg(isError, errorMsg);
	}


}
