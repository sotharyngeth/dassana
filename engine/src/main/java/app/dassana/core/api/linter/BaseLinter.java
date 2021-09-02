package app.dassana.core.api.linter;

import app.dassana.core.api.DassanaWorkflowValidationException;
import app.dassana.core.contentmanager.ContentManager;
import org.apache.commons.io.FileUtils;
import org.yaml.snakeyaml.Yaml;
import javax.inject.Singleton;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public abstract class BaseLinter {

	protected Yaml yaml = new Yaml();

	public abstract void loadTemplate(String path) throws IOException;

	public abstract void validate() throws IOException;

	protected boolean isPolicyContext(Map<String, Object> data){
		return ContentManager.POLICY_CONTEXT.equals(data.get(ContentManager.FIELDS.TYPE.getName()));
	}

	protected boolean isResourceContext(Map<String, Object> data){
		return ContentManager.RESOURCE_CONTEXT.equals(data.get(ContentManager.FIELDS.TYPE.getName()));
	}

	public List<File> loadFilesFromPath(String path, String[] extensions) {
		File dir = new File(path);
		List<File> files = (List<File>) FileUtils.listFiles(dir, extensions, true);
		return files;
	}
}
