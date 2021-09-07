package app.dassana.core.workflow.processor;

import static app.dassana.core.contentmanager.ContentManager.GENERAL_CONTEXT;
import static app.dassana.core.contentmanager.ContentManager.NORMALIZE;
import static app.dassana.core.contentmanager.ContentManager.POLICY_CONTEXT;
import static app.dassana.core.contentmanager.ContentManager.POLICY_CONTEXT_CAT;
import static app.dassana.core.contentmanager.ContentManager.POLICY_CONTEXT_SUB_CAT;
import static app.dassana.core.contentmanager.ContentManager.RESOURCE_CONTEXT;
import static app.dassana.core.contentmanager.ContentManager.WORKFLOW_ID;

import app.dassana.core.contentmanager.ContentManagerApi;
import app.dassana.core.launch.model.Request;
import app.dassana.core.policycontext.model.PolicyContext;
import app.dassana.core.resource.model.ResourceContext;
import app.dassana.core.risk.model.CombinedRisk;
import app.dassana.core.workflow.model.WorkflowOutputWithRisk;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.json.JSONObject;

@Singleton
public class Decorator {

  @Inject
  ContentManagerApi contentManagerApi;
  public static final String DASSANA_KEY = "dassana";
  public static final String TIME_TAKEN_KEY = "timeTakenInSec";
  public static final String WORKFLOW_TYPE_KEY = "workflowType";
  public static final String OUTPUT_KEY = "output";
  public static final String STEP_OUTPUT_KEY = "step-output";
  public static final String RISK_VALUE_KEY = "riskValue";
  public static final String CONDITION_KEY = "condition";
  public static final String RISK_NAME_KEY = "name";
  public static final String RISK_KEY = "risk";

  //todo: refactor to make it readable and maintainable
  public String getDassanaDecoratedJson(Request request,
      WorkflowOutputWithRisk normalizationOutput,
      Optional<WorkflowOutputWithRisk> policyContextWorkflowOutput,
      Optional<WorkflowOutputWithRisk> resourceContextWorkflowOutput,
      Optional<WorkflowOutputWithRisk> generalContextWorkflowOutput) throws Exception {

    CombinedRisk combinedRisk = new CombinedRisk();

    //put the output back in original data
    var messageBody = new JSONObject(request.getInputJson());
    Map<String, Object> dassanaMap = new HashMap<>();
    //handle normalization decoration
    JSONObject jsonObjectForNormalization = new JSONObject();
    if (request.isIncludeStepOutput()) {
      jsonObjectForNormalization.put(STEP_OUTPUT_KEY, normalizationOutput.getStepOutput());
    }
    jsonObjectForNormalization.put(OUTPUT_KEY, normalizationOutput.getOutput());
    jsonObjectForNormalization.put(WORKFLOW_ID, normalizationOutput.getWorkflowId());
    jsonObjectForNormalization.put(WORKFLOW_TYPE_KEY, NORMALIZE);
    jsonObjectForNormalization.put(TIME_TAKEN_KEY, normalizationOutput.getTimeTaken() / 1000);
    dassanaMap.put("normalize", jsonObjectForNormalization);
    if (generalContextWorkflowOutput.isPresent()) {
      JSONObject generalContextJsonObj = new JSONObject();
      generalContextJsonObj.put(WORKFLOW_ID, generalContextWorkflowOutput.get().getWorkflowId());
      generalContextJsonObj.put(WORKFLOW_TYPE_KEY, GENERAL_CONTEXT);
      generalContextJsonObj.put(OUTPUT_KEY, generalContextWorkflowOutput.get().getOutput());
      if (request.isIncludeStepOutput()) {
        generalContextJsonObj.put(STEP_OUTPUT_KEY, generalContextWorkflowOutput.get().getStepOutput());
      }
      Map<String, Object> riskObj = new HashMap<>();
      riskObj.put(RISK_VALUE_KEY, generalContextWorkflowOutput.get().getRisk().getRiskValue());
      riskObj.put(CONDITION_KEY, generalContextWorkflowOutput.get().getRisk().getCondition());
      riskObj.put(RISK_NAME_KEY, generalContextWorkflowOutput.get().getRisk().getName());
      generalContextJsonObj.put(RISK_KEY, riskObj);
      generalContextJsonObj.put(TIME_TAKEN_KEY, generalContextWorkflowOutput.get().getTimeTaken() / 1000);
      dassanaMap.put(GENERAL_CONTEXT, generalContextJsonObj);
      combinedRisk.setGeneralContextRisk(generalContextWorkflowOutput.get().getRisk());
    }

    if (policyContextWorkflowOutput.isPresent()) {
      PolicyContext policyContext = (PolicyContext) contentManagerApi.getWorkflowIdToWorkflowMap(request)
          .get(policyContextWorkflowOutput.get().getWorkflowId());

      JSONObject jsonObject = new JSONObject();
      jsonObject.put(WORKFLOW_ID, policyContext.getId());
      jsonObject.put(WORKFLOW_TYPE_KEY, POLICY_CONTEXT);
      jsonObject.put(POLICY_CONTEXT_CAT, policyContext.getCategory());
      jsonObject.put(POLICY_CONTEXT_SUB_CAT, policyContext.getSubCategory());
      jsonObject.put(OUTPUT_KEY, policyContextWorkflowOutput.get().getOutput());
      if (request.isIncludeStepOutput()) {
        jsonObject.put(STEP_OUTPUT_KEY, policyContextWorkflowOutput.get().getStepOutput());
      }

      Map<String, Object> riskObj = new HashMap<>();
      riskObj.put(RISK_VALUE_KEY, policyContextWorkflowOutput.get().getRisk().getRiskValue());
      riskObj.put(CONDITION_KEY, policyContextWorkflowOutput.get().getRisk().getCondition());
      riskObj.put(RISK_NAME_KEY, policyContextWorkflowOutput.get().getRisk().getName());
      jsonObject.put(RISK_KEY, riskObj);
      jsonObject.put(TIME_TAKEN_KEY, policyContextWorkflowOutput.get().getTimeTaken() / 1000);
      dassanaMap.put(POLICY_CONTEXT, jsonObject);
      combinedRisk.setGeneralContextRisk(policyContextWorkflowOutput.get().getRisk());

    }

    if (resourceContextWorkflowOutput.isPresent()) {
      ResourceContext resourceContext = (ResourceContext) contentManagerApi.getWorkflowIdToWorkflowMap(request)
          .get(resourceContextWorkflowOutput.get().getWorkflowId());

      JSONObject jsonObject = new JSONObject();
      jsonObject.put(WORKFLOW_ID, resourceContext.getId());
      jsonObject.put("workflowType", RESOURCE_CONTEXT);
      jsonObject.put("output", resourceContextWorkflowOutput.get().getOutput());
      if (request.isIncludeStepOutput()) {
        jsonObject.put("step-output", resourceContextWorkflowOutput.get().getStepOutput());
      }

      Map<String, Object> riskObj = new HashMap<>();
      riskObj.put("riskValue", resourceContextWorkflowOutput.get().getRisk().getRiskValue());
      riskObj.put("condition", resourceContextWorkflowOutput.get().getRisk().getCondition());
      riskObj.put("name", resourceContextWorkflowOutput.get().getRisk().getName());
      jsonObject.put("risk", riskObj);
      jsonObject.put("timeTakenInSec", resourceContextWorkflowOutput.get().getTimeTaken() / 1000);
      dassanaMap.put(RESOURCE_CONTEXT, jsonObject);
      combinedRisk.setResourceContextRisk(resourceContextWorkflowOutput.get().getRisk());

    }

    messageBody.put(DASSANA_KEY, dassanaMap);

    return messageBody.toString();


  }


}
