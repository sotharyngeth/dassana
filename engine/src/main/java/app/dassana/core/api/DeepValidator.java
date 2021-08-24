package app.dassana.core.api;

import app.dassana.core.api.linter.DassanaLinter;
import app.dassana.core.api.linter.PolicyLinter;

import javax.inject.Singleton;
import java.io.IOException;


/**
 * This validator checks if the content in the "content" dir is kosher or not
 */
@Singleton
public class DeepValidator {

  public void validate() throws IOException {
    DassanaLinter policy = new PolicyLinter();
    policy.validate();
  }
}
