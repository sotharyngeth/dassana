package app.dassana.core.api.linter;

import app.dassana.core.api.ValidationException;
import app.dassana.core.contentmanager.ContentManager;
import com.google.gson.Gson;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ActionsLinter extends BaseLinter {

	public final static String actionTemplatePath = "/actions";
	private Set<String> template = new HashSet<>();
	private Gson gson = new Gson();

	@Override
	public void loadTemplate(String path) throws FileNotFoundException {
		List<File> files = loadFilesFromPath(path, new String[]{"yaml"});
		for(File file : files){
			Map<String, Object> data = yaml.load(new FileInputStream(file));
			template.add((String)data.get("id"));
		}
	}

	@Override
	public void validate() throws IOException {
		String content = Thread.currentThread().getContextClassLoader().getResource("content").getFile();
		loadTemplate(content + actionTemplatePath);
		validateActions(content + "/workflows/csp");
	}


	private StatusMsg checkStepsForErrors(List<Map<String, Object>> steps){
		boolean isError = false;
		String errorMsg = null;
		for(int i = 0; i < steps.size() && !isError; i++){
			Map<String, Object> step = steps.get(i);
			if(!template.contains(step.get(ContentManager.FIELDS.USES.getName()))){
				isError  = true;
				errorMsg = "Invalid uses field: [" +  step.get(ContentManager.FIELDS.USES.getName()) + "]";
			}
		}
		return new StatusMsg(isError, errorMsg);
	}

	private StatusMsg validateYaml(Map<String, Object> data) {
		StatusMsg statusMsg = new StatusMsg(false);
		if(data.containsKey(ContentManager.FIELDS.STEPS.getName())){
			List<Map<String, Object>> steps = (List<Map<String, Object>>) data.get(ContentManager.FIELDS.STEPS.getName());
			statusMsg = checkStepsForErrors(steps);
		}
		return statusMsg;
	}

	public StatusMsg validateActionsAPI(String json){
		Map<String, Object> data = gson.fromJson(json, Map.class);
		StatusMsg statusMsg = validateYaml(data);
		return statusMsg;
	}

	private void validateActions(String path) throws IOException {
		List<File> files = loadFilesFromPath(path,new String[]{"yaml"});
		for(File file : files){
			Map<String, Object> data = yaml.load(new FileInputStream(file));
			StatusMsg statusMsg = validateYaml(data);
			if(statusMsg.isError()){
				throw new ValidationException(statusMsg.getMsg() + " in file: " + file.getName());
			}
		}
	}

}
