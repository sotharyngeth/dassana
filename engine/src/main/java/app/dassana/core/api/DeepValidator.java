package app.dassana.core.api;

import app.dassana.core.api.linter.*;

import javax.inject.Singleton;
import java.io.IOException;


/**
 * This validator checks if the content in the "content" dir is kosher or not
 */
@Singleton
public class DeepValidator {

  public void validate() throws IOException {
    BaseLinter policyLint = new PolicyLinter(), actionsLint = new ActionsLinter(),
            vendorLint = new VendorLinter(), resourceLint = new ResourceLinter();

    policyLint.validate();
    vendorLint.validate();
    actionsLint.validate();
    resourceLint.validate();
  }
}
