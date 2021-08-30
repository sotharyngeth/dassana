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

public abstract class BaseLinter {

	protected class ErrorMsg {
		private boolean isError;
		private String msg;

		public ErrorMsg(boolean isError){
			this.isError = isError;
		}

		public ErrorMsg(boolean isError, String field) {
			this.isError = isError;
			this.msg = field;
		}

		public boolean isError() {
			return isError;
		}

		public void setError(boolean error) {
			isError = error;
		}

		public String getMsg() {
			return msg;
		}

		public void setMsg(String msg) {
			this.msg = msg;
		}
	}

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
