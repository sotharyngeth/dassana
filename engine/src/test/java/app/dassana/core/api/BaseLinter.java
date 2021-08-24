package app.dassana.core.api;

import app.dassana.core.api.linter.ActionsLinter;
import app.dassana.core.api.linter.PolicyLinter;
import app.dassana.core.api.linter.ResourceLinter;
import app.dassana.core.api.linter.VendorLinter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.fail;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BaseLinter {


	@Test
	//req 3 - 64
	public void vendorValidate(){
		try {
			VendorLinter vendorLinter = new VendorLinter();
			vendorLinter.validate();
		} catch (IOException e) {
			fail(e.getMessage());
		}
	}

	@Test
	//req #107
	public void actionValidate(){
		try {
			ActionsLinter actionsLinter = new ActionsLinter();
			actionsLinter.validate();
		} catch (IOException e) {
			fail(e.getMessage());
		}
	}

	@Test
	//req #88
	public void policyValidate(){
		try {
			PolicyLinter policyLinter = new PolicyLinter();
			policyLinter.validate();
		} catch (IOException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void resourceValidate(){
		ResourceLinter resourceLinter = new ResourceLinter();
		try {
			resourceLinter.validate();
		} catch (IOException e) {
			fail(e.getMessage());
		}
	}

}
