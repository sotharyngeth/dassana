package app.dassana.core.api;

import app.dassana.core.api.linter.ActionsLinter;
import app.dassana.core.api.linter.PolicyLinter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import java.io.FileNotFoundException;
import java.io.IOException;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class VendorLinter {

	app.dassana.core.api.linter.VendorLinter vendorLinter = new app.dassana.core.api.linter.VendorLinter();
	String content = Thread.currentThread().getContextClassLoader().getResource("content").getFile();

	@BeforeAll
	public void loadTemplate(){
		try {
			vendorLinter.loadTemplate(content + "/schemas/vendors/vendor-list.yaml");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	@Test
	//req 3 - 64
	public void validateKeysExist(){
		try {
			vendorLinter.validateKeysExist(content + "/workflows/vendors");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	//req 3 - 64
	public void vendorValidate(){
		try {
			vendorLinter.validate();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	//req 4 - 107
	public void actionValidate(){
		try {
			ActionsLinter actionsLinter = new ActionsLinter();
			actionsLinter.loadTemplate(content + "/actions");
			actionsLinter.validate();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void policyValidate(){
		try {
			PolicyLinter policyLinter = new PolicyLinter();
			policyLinter.validate();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
