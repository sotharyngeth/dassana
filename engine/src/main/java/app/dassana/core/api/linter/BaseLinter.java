package app.dassana.core.api.linter;

import org.apache.commons.io.FileUtils;
import org.yaml.snakeyaml.Yaml;

import javax.inject.Singleton;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Singleton
public abstract class BaseLinter {

	protected Yaml yaml = new Yaml();

	public abstract void loadTemplate(String path) throws IOException;

	public abstract void validate() throws IOException;

	public List<Map<String, Object>> extractYamlArray(File file, String name) throws FileNotFoundException {
		Map<String, Object> map = yaml.load(new FileInputStream(file));
		List<Map<String, Object>> arr = (List<Map<String, Object>>) map.get(name);
		return arr;
	}

	public List<File> loadFilesFromPath(String path, String[] extensions) {
		File dir = new File(path);
		List<File> files = (List<File>) FileUtils.listFiles(dir, extensions, true);
		return files;
	}
}
