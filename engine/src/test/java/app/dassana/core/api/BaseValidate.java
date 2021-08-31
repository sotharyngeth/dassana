package app.dassana.core.api;

import app.dassana.core.api.linter.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.fail;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BaseValidate {


	@Test
	public void vendorValidate(){
		try {
			BaseLinter linter = new VendorLinter();
			linter.validate();
		} catch (IOException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void actionValidate(){
		try {
			BaseLinter linter = new ActionsLinter();
			linter.validate();
		} catch (IOException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void policyValidate(){
		try {
			BaseLinter linter = new PolicyLinter();
			linter.validate();
		} catch (IOException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void resourceValidate(){
		try {
			BaseLinter linter = new ResourceLinter();
			linter.validate();
		} catch (IOException e) {
			fail(e.getMessage());
		}
	}

}
