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
import java.util.stream.Collectors;
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
  private NormalizeLinter normalizeLinter = new NormalizeLinter();
  private GeneralLinter generalLinter = new GeneralLinter();
  private ResourceLinter resourceLinter = new ResourceLinter();
  private PolicyLinter policyLinter = new PolicyLinter();

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

  private void initLinters() throws IOException{
    normalizeLinter.init();
    generalLinter.init();
    resourceLinter.init();
    policyLinter.init();
  }

  private void processJson(BaseLinter linter, String json) throws IOException {
    List<String> issues = linter.validate(json);
    if(issues.size() > 0){
      DassanaWorkflowValidationException workflowException = new DassanaWorkflowValidationException();
      workflowException.setIssues(issues);
      throw workflowException;
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
    initLinters();

    if (workflow instanceof NormalizerWorkflow) {
      String normalizerSchema = IOUtils.toString(Thread.currentThread().getContextClassLoader()
          .getResourceAsStream("content/schemas/normalizer-schema.json"), Charset.defaultCharset());
      validateJsonAgainstJsonSchema(workflowAsJson, normalizerSchema);
      processJson(normalizeLinter, workflowAsJson);
    } else if (workflow instanceof PolicyContext) {
      String generalContextSchema = IOUtils.toString(Thread.currentThread().getContextClassLoader()
          .getResourceAsStream("content/schemas/risk-schema.json"), Charset.defaultCharset());
      validateJsonAgainstJsonSchema(workflowAsJson, generalContextSchema);
      processJson(policyLinter, workflowAsJson);
    } else if (workflow instanceof ResourceContext) {
      String resourceContextSchema = IOUtils.toString(Thread.currentThread().getContextClassLoader()
          .getResourceAsStream("content/schemas/resource-context-schema.json"), Charset.defaultCharset());
      validateJsonAgainstJsonSchema(workflowAsJson, resourceContextSchema);
      processJson(resourceLinter, workflowAsJson);
    } else if (workflow instanceof GeneralContext) {
      String generalContextSchema = IOUtils.toString(Thread.currentThread().getContextClassLoader()
          .getResourceAsStream("content/schemas/risk-schema.json"), Charset.defaultCharset());
      validateJsonAgainstJsonSchema(workflowAsJson, generalContextSchema);
      processJson(generalLinter, workflowAsJson);
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
