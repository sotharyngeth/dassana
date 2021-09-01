package app.dassana.core.api;

import app.dassana.core.api.linter.*;
import app.dassana.core.contentmanager.ContentManager;
import app.dassana.core.normalize.model.NormalizerWorkflow;
import app.dassana.core.policycontext.model.PolicyContext;
import app.dassana.core.resource.model.GeneralContext;
import app.dassana.core.resource.model.ResourceContext;
import app.dassana.core.rule.RuleMatch;
import app.dassana.core.workflow.model.Filter;
import app.dassana.core.workflow.model.Workflow;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.commons.io.IOUtils;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONException;
import org.json.JSONObject;

@Singleton
public class WorkflowValidator {

  @Inject ContentManager contentManager;
  @Inject RuleMatch ruleMatch;
  private SchemaLoader schemaLoader;
  private ResourceLinter resourceLinter = new ResourceLinter();
  private PolicyLinter policyLinter = new PolicyLinter();
  private ActionsLinter actionsLinter = new ActionsLinter();
  private VendorLinter vendorLinter = new VendorLinter();


  void validateJsonAgainstJsonSchema(String json, String jsonSchema) {

    JSONObject baseSchema = new JSONObject(jsonSchema);
    Schema schema = SchemaLoader.load(baseSchema);
    try {
      schema.validate(new JSONObject(json));
    } catch (org.everit.json.schema.ValidationException e) {
      List<String> issues = new LinkedList<>();
      List<ValidationException> causingExceptions = e.getCausingExceptions();
      causingExceptions.forEach(e1 -> issues.add(e1.getMessage()));
      DassanaWorkflowValidationException dassanaWorkflowValidationException = new DassanaWorkflowValidationException();
      dassanaWorkflowValidationException.setIssues(issues);
      if (dassanaWorkflowValidationException.getIssues().size() > 0) {
        throw dassanaWorkflowValidationException;
      }
    }


  }

  private void loadTemplates() throws IOException{
    String content = Thread.currentThread().getContextClassLoader().getResource("content").getFile();
    resourceLinter.loadTemplate(content + "/schemas/resource-hierarchy/resource-hierarchy.yaml");
    policyLinter.loadTemplate(content + "/schemas/policy-classification/policy-classification.yaml");
    vendorLinter.loadTemplate(content + "/schemas/vendors/vendor-list.yaml");
    actionsLinter.loadTemplate(content + "/actions");
  }

  private void appendToListIfError(List<String> issues, StatusMsg statusMsg){
    if(statusMsg.isError()){
      issues.add(statusMsg.getMsg());
    }
  }

  private void validatePolicies(String json){
    DassanaWorkflowValidationException exception = new DassanaWorkflowValidationException("failed during policy validation");

    List<String> issues = new ArrayList<>();
    appendToListIfError(issues, resourceLinter.validateResourcesAPI(json));
    appendToListIfError(issues, vendorLinter.validateFilterAPI(json));
    appendToListIfError(issues, actionsLinter.validateActionsAPI(json));
    appendToListIfError(issues, policyLinter.validatePoliciesAPI(json));
    exception.setIssues(issues);

    if(issues.size() > 0) {
      throw exception;
    }
  }

  private void validateResources(String json){
    DassanaWorkflowValidationException exception = new DassanaWorkflowValidationException("failed during resources validation");

    List<String> issues = new ArrayList<>();
    appendToListIfError(issues, resourceLinter.validateResourcesAPI(json));
    appendToListIfError(issues, actionsLinter.validateActionsAPI(json));
    exception.setIssues(issues);

    if(issues.size() > 0) {
      throw exception;
    }
  }

  private void validateNormalize(String json){
    DassanaWorkflowValidationException exception = new DassanaWorkflowValidationException("failed during resources validation");
    List<String> issues = new ArrayList<>();
    appendToListIfError(issues, resourceLinter.validateResourcesAPI(json));
    if(issues.size() > 0) {
      throw exception;
    }
  }

  public void handleValidate(String workflowAsJson) throws IOException {

    Workflow workflow;
    try {
      JSONObject jsonObject = new JSONObject(workflowAsJson);
      workflow = contentManager.getWorkflow(jsonObject);
    } catch (JSONException e) {
      DassanaWorkflowValidationException dassanaWorkflowValidationException = new DassanaWorkflowValidationException();
      List<String> messages = new LinkedList<>();
      messages.add(e.getMessage());
      dassanaWorkflowValidationException.setIssues(messages);
      throw dassanaWorkflowValidationException;
    }

    //let's validate against base schema first
    String baseSchema = IOUtils.toString(Thread.currentThread().getContextClassLoader()
        .getResourceAsStream("content/schemas/base-workflow-schema.json"), Charset.defaultCharset());

    validateJsonAgainstJsonSchema(workflowAsJson, baseSchema);

    loadTemplates();

    if (workflow instanceof NormalizerWorkflow) {
      String normalizerSchema = IOUtils.toString(Thread.currentThread().getContextClassLoader()
          .getResourceAsStream("content/schemas/normalizer-schema.json"), Charset.defaultCharset());
      validateJsonAgainstJsonSchema(workflowAsJson, normalizerSchema);
      validateNormalize(workflowAsJson);

    } else if (workflow instanceof PolicyContext) {
      String generalContextSchema = IOUtils.toString(Thread.currentThread().getContextClassLoader()
          .getResourceAsStream("content/schemas/risk-schema.json"), Charset.defaultCharset());
      validateJsonAgainstJsonSchema(workflowAsJson, generalContextSchema);
      validatePolicies(workflowAsJson);
    } else if (workflow instanceof ResourceContext) {
      String resourceContextSchema = IOUtils.toString(Thread.currentThread().getContextClassLoader()
          .getResourceAsStream("content/schemas/resource-context-schema.json"), Charset.defaultCharset());
      validateJsonAgainstJsonSchema(workflowAsJson, resourceContextSchema);
      validateResources(workflowAsJson);
    } else if (workflow instanceof GeneralContext) {
      String generalContextSchema = IOUtils.toString(Thread.currentThread().getContextClassLoader()
          .getResourceAsStream("content/schemas/risk-schema.json"), Charset.defaultCharset());
      validateJsonAgainstJsonSchema(workflowAsJson, generalContextSchema);
    }

    for (Filter filter : workflow.getFilters()) {
      List<String> rules = filter.getRules();
      for (String s : rules) {
        if (!ruleMatch.isValidRule(s)) {
          throw new DassanaWorkflowValidationException(String.format("Sorry, the rule %s isn't valid", s));
        }
      }
    }

  }

}
