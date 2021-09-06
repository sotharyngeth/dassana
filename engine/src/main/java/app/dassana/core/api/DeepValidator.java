package app.dassana.core.api;

import app.dassana.core.api.linter.*;
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


/**
 * This validator checks if the content in the "content" dir is kosher or not
 */
@Singleton
public class DeepValidator {

  Yaml yaml = new Yaml();
  NormalizeLinter normalizeLinter = new NormalizeLinter();
  GeneralLinter generalLinter = new GeneralLinter();
  ResourceLinter resourceLinter = new ResourceLinter();
  PolicyLinter policyLinter = new PolicyLinter();
  String content = Thread.currentThread().getContextClassLoader().getResource("content").getFile();

  private void initLinters() throws IOException {
    normalizeLinter.init();
    generalLinter.init();
    resourceLinter.init();
    policyLinter.init();
  }

  private void validateYaml(Map<String, Object> data, String type, String filename) throws IOException {
    if(ContentManager.NORMALIZE.equals(type)){
      normalizeLinter.validate(data, filename);
    }else if(ContentManager.GENERAL_CONTEXT.equals(type)){
      generalLinter.validate(data, filename);
    }else if(ContentManager.RESOURCE_CONTEXT.equals(type)){
      resourceLinter.validate(data, filename);
    }else if(ContentManager.POLICY_CONTEXT.equals(type)){
      policyLinter.validate(data, filename);
    }
  }

  private void processYaml() throws IOException {
    List<File> files = BaseLinter.loadFilesFromPath(content + "/workflows", new String[]{"yaml"});
    for(File file : files){
      Map<String, Object> data = yaml.load(new FileInputStream(file));
      String type = (String) data.get("type");
      validateYaml(data, type, file.getName());
    }
  }

  public void validate() throws IOException {
    initLinters();
    processYaml();
  }
}
