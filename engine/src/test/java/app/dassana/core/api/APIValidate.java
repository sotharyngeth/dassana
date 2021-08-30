package app.dassana.core.api;

import app.dassana.core.api.linter.ActionsLinter;
import app.dassana.core.api.linter.PolicyLinter;
import app.dassana.core.api.linter.ResourceLinter;
import app.dassana.core.api.linter.VendorLinter;
import app.dassana.core.launch.Helper;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import javax.inject.Inject;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest
public class APIValidate {

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
	public void badInputNormAPI() {
		ValidationException exception = Assertions.assertThrows(ValidationException.class, () -> {
			VendorLinter vendorLinter = new VendorLinter();
			vendorLinter.loadTemplate(content + "/schemas/vendors/vendor-list.yaml");
			String json = helper.getFileContent("inputs/invalidVendor.json");
			vendorLinter.validateRequiredFieldsAPI(json);
		});
		assertTrue(exception.getMessage().contains("canonicalId, alertId"));
	}

	@Test
	public void validateFilterAPI() throws IOException {
		VendorLinter vendorLinter = new VendorLinter();
		vendorLinter.loadTemplate(content + "/schemas/vendors/vendor-list.yaml");
		String json = helper.getFileContent("inputs/validPolicy.json");
		vendorLinter.validateFilterAPI(json);
	}

	@Test
	public void badFilterAPI(){
		ValidationException exception = Assertions.assertThrows(ValidationException.class, () -> {
			VendorLinter vendorLinter = new VendorLinter();
			vendorLinter.loadTemplate(content + "/schemas/vendors/vendor-list.yaml");
			String json = helper.getFileContent("inputs/invalidPolicy.json");
			vendorLinter.validateFilterAPI(json);
		});
		assertTrue(exception.getMessage().contains("aws-config1"));
	}

	@Test
	public void validateResourcesAPI() throws IOException {
		ResourceLinter resourceLinter = new ResourceLinter();
		resourceLinter.loadTemplate(content + "/schemas/resource-hierarchy/resource-hierarchy.yaml");
		String json = helper.getFileContent("inputs/validPolicy.json");
		resourceLinter.validateResourcesAPI(json);
	}

	@Test
	public void badResourcesAPI(){
		ValidationException exception = Assertions.assertThrows(ValidationException.class, () -> {
			ResourceLinter resourceLinter = new ResourceLinter();
			resourceLinter.loadTemplate(content + "/schemas/resource-hierarchy/resource-hierarchy.yaml");
			String json = helper.getFileContent("inputs/invalidPolicy.json");
			resourceLinter.validateResourcesAPI(json);
		});
		assertTrue(exception.getMessage().contains("aws1"));
	}

	@Test
	public void validateActionsAPI() throws IOException {
		ActionsLinter actionsLinter = new ActionsLinter();
		actionsLinter.loadTemplate(content + "/actions");
		String json = helper.getFileContent("inputs/validPolicy.json");
		actionsLinter.validateActionsAPI(json);
	}

	@Test
	public void badActionsAPI(){
		ValidationException exception = Assertions.assertThrows(ValidationException.class, () -> {
			ActionsLinter actionsLinter = new ActionsLinter();
			actionsLinter.loadTemplate(content + "/actions");
			String json = helper.getFileContent("inputs/invalidPolicy.json");
			actionsLinter.validateActionsAPI(json);
		});
		assertTrue(exception.getMessage().contains("invalid"));
	}

	@Test
	public void validatePoliciesAPI() throws IOException {
		PolicyLinter policyLinter = new PolicyLinter();
		policyLinter.loadTemplate(content + "/schemas/policy-classification/policy-classification.yaml");
		String json = helper.getFileContent("inputs/validPolicy.json");
		policyLinter.validatePoliciesAPI(json);
	}

	@Test
	public void badPoliciesAPI(){
		ValidationException exception = Assertions.assertThrows(ValidationException.class, () -> {
			PolicyLinter policyLinter = new PolicyLinter();
			policyLinter.loadTemplate(content + "/schemas/policy-classification/policy-classification.yaml");
			String json = helper.getFileContent("inputs/invalidPolicy.json");
			policyLinter.validatePoliciesAPI(json);
		});
		assertTrue(exception.getMessage().contains("secrets1"));
	}

}
