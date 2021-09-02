package app.dassana.core.api;

import app.dassana.core.api.linter.*;
import app.dassana.core.launch.Helper;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import javax.inject.Inject;
import static org.junit.jupiter.api.Assertions.assertFalse;

@MicronautTest
public class APIValidate {

	String content = Thread.currentThread().getContextClassLoader().getResource("content").getFile();

	@Inject
	Helper helper;

	@Test
	public void validateNormAPI(){
		VendorLinter vendorLinter = new VendorLinter();
		try {
			vendorLinter.loadTemplate(content + "/schemas/vendors/vendor-list.yaml");
			String json = helper.getFileContent("inputs/validVendor.json");
			StatusMsg statusMsg = vendorLinter.validateVendorIdAPI(json);
			if(statusMsg.isError()){
				Assertions.fail(statusMsg.getMsg());
			}
		}catch (Exception e){
			Assertions.fail(e.getMessage());
		}
	}

	@Test
	public void badInputNormAPI() {
		VendorLinter vendorLinter = new VendorLinter();
		try {
			vendorLinter.loadTemplate(content + "/schemas/vendors/vendor-list.yaml");
			String json = helper.getFileContent("inputs/invalidVendor.json");
			StatusMsg statusMsg = vendorLinter.validateVendorIdAPI(json);
			assertFalse(!statusMsg.getMsg().contains("foo-cloud-cspm-vendor1"));
		}catch (Exception e){
			Assertions.fail(e.getMessage());
		}
	}

	@Test
	public void validateFilterAPI(){
		VendorLinter vendorLinter = new VendorLinter();
		try {
			vendorLinter.loadTemplate(content + "/schemas/vendors/vendor-list.yaml");
			String json = helper.getFileContent("inputs/validPolicy.json");
			StatusMsg statusMsg = vendorLinter.validateFilterAPI(json);
			if(statusMsg.isError()){
				Assertions.fail(statusMsg.getMsg());
			}
		}catch (Exception e){
			Assertions.fail(e.getMessage());
		}
	}

	@Test
	public void badFilterAPI(){
		VendorLinter vendorLinter = new VendorLinter();
		try {
			vendorLinter.loadTemplate(content + "/schemas/vendors/vendor-list.yaml");
			String json = helper.getFileContent("inputs/invalidPolicy.json");
			StatusMsg statusMsg = vendorLinter.validateFilterAPI(json);
			assertFalse(!statusMsg.getMsg().contains("aws-config1"));
		}catch (Exception e){
			Assertions.fail(e.getMessage());
		}
	}

	@Test
	public void validateResourcesAPI(){
		ResourceLinter resourceLinter = new ResourceLinter();
		try {
			resourceLinter.loadTemplate(content + "/schemas/resource-hierarchy/resource-hierarchy.yaml");
			String json = helper.getFileContent("inputs/validPolicy.json");
			StatusMsg statusMsg = resourceLinter.validateResourcesAPI(json);
			if(statusMsg.isError()){
				Assertions.fail(statusMsg.getMsg());
			}
		}catch (Exception e){
			Assertions.fail(e.getMessage());
		}
	}

	@Test
	public void badResourcesAPI(){
		ResourceLinter resourceLinter = new ResourceLinter();
		try {
			resourceLinter.loadTemplate(content + "/schemas/resource-hierarchy/resource-hierarchy.yaml");
			String json = helper.getFileContent("inputs/invalidPolicy.json");
			StatusMsg statusMsg = resourceLinter.validateResourcesAPI(json);
			assertFalse(!statusMsg.getMsg().contains("aws1"));
		}catch (Exception e){
			Assertions.fail(e.getMessage());
		}
	}

	@Test
	public void validateActionsAPI(){
		ActionsLinter actionsLinter = new ActionsLinter();
		try {
			actionsLinter.loadTemplate(content + "/actions");
			String json = helper.getFileContent("inputs/validPolicy.json");
			StatusMsg statusMsg = actionsLinter.validateActionsAPI(json);
			if(statusMsg.isError()){
				Assertions.fail(statusMsg.getMsg());
			}
		}catch (Exception e){
			Assertions.fail(e.getMessage());
		}
	}

	@Test
	public void badActionsAPI(){
		ActionsLinter actionsLinter = new ActionsLinter();
		try {
			actionsLinter.loadTemplate(content + "/actions");
			String json = helper.getFileContent("inputs/invalidPolicy.json");
			StatusMsg statusMsg = actionsLinter.validateActionsAPI(json);
			assertFalse(!statusMsg.getMsg().contains("invalid"));
		}catch (Exception e){
			Assertions.fail(e.getMessage());
		}
	}

	@Test
	public void validatePoliciesAPI(){
		PolicyLinter policyLinter = new PolicyLinter();
		try {
			policyLinter.loadTemplate(content + "/schemas/policy-classification/policy-classification.yaml");
			String json = helper.getFileContent("inputs/validPolicy.json");
			StatusMsg statusMsg = policyLinter.validatePoliciesAPI(json);
			if(statusMsg.isError()){
				Assertions.fail(statusMsg.getMsg());
			}
		}catch (Exception e){
			Assertions.fail(e.getMessage());
		}
	}

	@Test
	public void badPoliciesAPI(){
		PolicyLinter policyLinter = new PolicyLinter();
		try{
			policyLinter.loadTemplate(content + "/schemas/policy-classification/policy-classification.yaml");
			String json = helper.getFileContent("inputs/invalidPolicy.json");
			StatusMsg statusMsg = policyLinter.validatePoliciesAPI(json);
			assertFalse(!statusMsg.getMsg().contains("secrets1"));
		}catch (Exception e){
			Assertions.fail(e.getMessage());
		}
	}

}
