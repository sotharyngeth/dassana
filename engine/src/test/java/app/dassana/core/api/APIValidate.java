package app.dassana.core.api;

import app.dassana.core.api.linter.*;
import app.dassana.core.launch.Helper;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

@MicronautTest
public class APIValidate {

	@Inject
	Helper helper;

	@Test
	public void validateNormalizeLinter(){
		NormalizeLinter normalizeLinter = new NormalizeLinter();
		try {
			normalizeLinter.init();
			String json = helper.getFileContent("inputs/validVendor.json");
			List<String> issues = normalizeLinter.validate(json);
			if(issues.size() > 0){
				Assertions.fail(issues.toString());
			}
		}catch (Exception e){
			Assertions.fail(e.getMessage());
		}
	}

	@Test
	public void validateGeneralLinter(){
		GeneralLinter generalLinter = new GeneralLinter();
		try {
			generalLinter.init();
			String json = helper.getFileContent("inputs/validPolicy.json");
			List<String> issues = generalLinter.validate(json);
			if(issues.size() > 0){
				Assertions.fail(issues.toString());
			}
		}catch (Exception e){
			Assertions.fail(e.getMessage());
		}
	}

	@Test
	public void validateResourceLinter(){
		ResourceLinter resourceLinter = new ResourceLinter();
		try {
			resourceLinter.init();
			String json = helper.getFileContent("inputs/validPolicy.json");
			List<String> issues = resourceLinter.validate(json);
			if(issues.size() > 0){
				Assertions.fail(issues.toString());
			}
		}catch (Exception e){
			Assertions.fail(e.getMessage());
		}
	}

	@Test
	public void validatePolicyLinter(){
		PolicyLinter policyLinter = new PolicyLinter();
		try {
			policyLinter.init();
			String json = helper.getFileContent("inputs/validPolicy.json");
			List<String> issues = policyLinter.validate(json);
			if(issues.size() > 0){
				Assertions.fail(issues.toString());
			}
		}catch (Exception e){
			Assertions.fail(e.getMessage());
		}
	}

}
