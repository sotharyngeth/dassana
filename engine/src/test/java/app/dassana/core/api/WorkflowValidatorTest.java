package app.dassana.core.api;

import app.dassana.core.contentmanager.ContentManager;
import app.dassana.core.util.StringyThings;
import com.google.gson.Gson;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import javax.inject.Inject;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@MicronautTest
class WorkflowValidatorTest {

  @Inject WorkflowValidator workflowValidator;
  @Inject ContentManager contentManager;
  @Inject Gson gson;

  @Test
  void handleValidate() throws Exception {

    String content = Thread.currentThread().getContextClassLoader().getResource("content/workflows").getFile();
    contentManager.getWorkflowsFromEmbeddedContentDir(new File(content));

    Files.walk(Paths.get(content))
        .filter(Files::isRegularFile)
        .forEach(path -> {
          try {
            String workflowYaml = FileUtils.readFileToString(path.toFile(), Charset.defaultCharset());

            try {
              workflowValidator.handleValidate(StringyThings.getJsonFromYaml(workflowYaml));
            } catch (Exception e) {
              if (e instanceof DassanaWorkflowValidationException) {
                DassanaWorkflowValidationException dassanaWorkflowValidationException = (DassanaWorkflowValidationException) e;
                List<String> issues = dassanaWorkflowValidationException.getIssues();
                for (String s : issues) {
                  Assertions.fail(s);
                }
              } else {
                Assertions.fail(e.getMessage());
              }
            }

          } catch (IOException e) {
            Assertions.fail(e);
          }


        });


  }
}