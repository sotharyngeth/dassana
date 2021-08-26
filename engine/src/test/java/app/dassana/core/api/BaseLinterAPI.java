package app.dassana.core.api;

import app.dassana.core.api.linter.ActionsLinter;
import app.dassana.core.api.linter.PolicyLinter;
import app.dassana.core.api.linter.ResourceLinter;
import app.dassana.core.api.linter.VendorLinter;
import app.dassana.core.launch.Helper;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;
import javax.inject.Inject;
import java.io.IOException;

@MicronautTest
public class BaseLinterAPI {

	String content = Thread.currentThread().getContextClassLoader().getResource("content").getFile();

	@Inject
	Helper helper;

	@Test
	public void validateNormAPI() throws IOException {
		VendorLinter vendorLinter = new VendorLinter();
		vendorLinter.loadTemplate(content + "/schemas/vendors/vendor-list.yaml");
		String json = helper.getFileContent("inputs/validVendor.json");
		vendorLinter.validateRequiredFieldsAPI(json);
	}

	@Test
	public void validateFilterAPI() throws IOException {
		VendorLinter vendorLinter = new VendorLinter();
		vendorLinter.loadTemplate(content + "/schemas/vendors/vendor-list.yaml");
		String json = helper.getFileContent("inputs/validPolicy.json");
		vendorLinter.validateFilterAPI(json);
	}

	@Test
	public void validateResourcesAPI() throws IOException {
		ResourceLinter resourceLinter = new ResourceLinter();
		resourceLinter.loadTemplate(content + "/schemas/resource-hierarchy/resource-hierarchy.yaml");
		String json = helper.getFileContent("inputs/validPolicy.json");
		resourceLinter.validateResourcesAPI(json);
	}

	@Test
	public void validateActionsAPI() throws IOException {
		ActionsLinter actionsLinter = new ActionsLinter();
		actionsLinter.loadTemplate(content + "/actions");
		String json = helper.getFileContent("inputs/validPolicy.json");
		actionsLinter.validateActionsAPI(json);
	}

	@Test
	public void validatePoliciesAPI() throws IOException {
		PolicyLinter policyLinter = new PolicyLinter();
		policyLinter.loadTemplate(content + "/schemas/policy-classification/policy-classification.yaml");
		String json = helper.getFileContent("inputs/validPolicy.json");
		policyLinter.validatePoliciesAPI(json);
	}

}
