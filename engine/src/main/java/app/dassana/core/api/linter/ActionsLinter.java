package app.dassana.core.api.linter;

import app.dassana.core.api.ValidationException;
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
		loadTemplate(content + "/actions");
		validateActions(content + "/workflows/csp");
	}


	private void validateYaml(Map<String, Object> data) {
		if(data.containsKey("steps")){
			List<Map<String, Object>> steps = (List<Map<String, Object>>) data.get("steps");
			for(Map<String, Object> step : steps){
				if(!template.contains(step.get("uses"))){
					throw new ValidationException("Invalid action: " + step.get("uses"));
				}
			}
		}
	}

	public void validateActionsAPI(String json){
		Map<String, Object> data = gson.fromJson(json, Map.class);
		validateYaml(data);
	}

	private void validateActions(String path) throws IOException {
		List<File> files = loadFilesFromPath(path,new String[]{"yaml"});
		for(File file : files){
			Map<String, Object> data = yaml.load(new FileInputStream(file));
			validateYaml(data);
		}
	}

}
