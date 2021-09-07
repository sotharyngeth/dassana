package app.dassana.core.api.linter;

import app.dassana.core.contentmanager.ContentManager;
import org.apache.commons.io.FileUtils;
import org.yaml.snakeyaml.Yaml;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class BaseLinter {

	private final static String helpLink = "https://www.google.com";
	protected String content = Thread.currentThread().getContextClassLoader().getResource("content").getFile();

	protected Yaml yaml = new Yaml();

	public abstract List<String> validate(String json) throws IOException;

	public abstract void validate(Map<String, Object> data, String filename) throws IOException;

	public abstract void init() throws IOException;

	protected StatusMsg validateNormVendor(ContentManager.FIELDS field, Map<String, Object> data, Set<String> template){
		String msg  = null;
		String name = field.getName();
		String val  = (String) data.get(field.getName());
		if(val == null){
			msg = "Required field [" + name + "] is missing, " + helpText(template);
		}else if(!template.contains(data.get(field.getName()))){
			msg = "Invalid " + name + " field [" + val + "], " + helpText(template);
		}
		boolean isError = msg == null ? false : true;
		return new StatusMsg(isError, msg);
	}

	protected String helpText(Set<String> set){
		return "available fields: " + set;
	}

	protected String helpText(Map<String, Set<String>> map, String key){
		return map.get(key).isEmpty() ?
						"no available fields please remove this line. For additional help see " + helpLink :
						"available fields: " + map.get(key) + ". For additional help see " + helpLink;
	}

	protected String helpText(Map<String, Set<String>> map) {
		return "available fields: " + map.keySet() + ". For additional help see " + helpLink;
	}

	public static List<File> loadFilesFromPath(String path, String[] extensions) {
		File dir = new File(path);
		List<File> files = (List<File>) FileUtils.listFiles(dir, extensions, true);
		return files;
	}
}
