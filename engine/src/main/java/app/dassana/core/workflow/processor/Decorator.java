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
  public static final String DASSANA_KEY="dassana";

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
      jsonObjectForNormalization.put("step-output", normalizationOutput.getStepOutput());
    }
    jsonObjectForNormalization.put("output", normalizationOutput.getOutput());
    jsonObjectForNormalization.put(WORKFLOW_ID, normalizationOutput.getWorkflowId());
    jsonObjectForNormalization.put("workflowType",NORMALIZE);
    dassanaMap.put("normalize", jsonObjectForNormalization);
    if (generalContextWorkflowOutput.isPresent()) {
      JSONObject generalContextJsonObj = new JSONObject();
      generalContextJsonObj.put(WORKFLOW_ID, generalContextWorkflowOutput.get().getWorkflowId());
      generalContextJsonObj.put("workflowType",GENERAL_CONTEXT);
      generalContextJsonObj.put("output", generalContextWorkflowOutput.get().getOutput());
      if (request.isIncludeStepOutput()) {
        generalContextJsonObj.put("step-output", generalContextWorkflowOutput.get().getStepOutput());
      }
      Map<String, Object> riskObj = new HashMap<>();
      riskObj.put("riskValue", generalContextWorkflowOutput.get().getRisk().getRiskValue());
      riskObj.put("condition", generalContextWorkflowOutput.get().getRisk().getCondition());
      riskObj.put("name", generalContextWorkflowOutput.get().getRisk().getName());
      generalContextJsonObj.put("risk", riskObj);
      dassanaMap.put(GENERAL_CONTEXT, generalContextJsonObj);
      combinedRisk.setGeneralContextRisk(generalContextWorkflowOutput.get().getRisk());
    }

    if (policyContextWorkflowOutput.isPresent()) {
      PolicyContext policyContext = (PolicyContext) contentManagerApi.getWorkflowIdToWorkflowMap(request)
          .get(policyContextWorkflowOutput.get().getWorkflowId());

      JSONObject jsonObject = new JSONObject();
      jsonObject.put(WORKFLOW_ID, policyContext.getId());
      jsonObject.put("workflowType",POLICY_CONTEXT);
      jsonObject.put(POLICY_CONTEXT_CAT, policyContext.getCategory());
      jsonObject.put(POLICY_CONTEXT_SUB_CAT, policyContext.getSubCategory());
      jsonObject.put("output", policyContextWorkflowOutput.get().getOutput());
      if (request.isIncludeStepOutput()) {
        jsonObject.put("step-output", policyContextWorkflowOutput.get().getStepOutput());
      }

      Map<String, Object> riskObj = new HashMap<>();
      riskObj.put("riskValue", policyContextWorkflowOutput.get().getRisk().getRiskValue());
      riskObj.put("condition", policyContextWorkflowOutput.get().getRisk().getCondition());
      riskObj.put("name", policyContextWorkflowOutput.get().getRisk().getName());
      jsonObject.put("risk", riskObj);
      dassanaMap.put(POLICY_CONTEXT, jsonObject);
      combinedRisk.setGeneralContextRisk(policyContextWorkflowOutput.get().getRisk());

    }

    if (resourceContextWorkflowOutput.isPresent()) {
      ResourceContext resourceContext = (ResourceContext) contentManagerApi.getWorkflowIdToWorkflowMap(request)
          .get(resourceContextWorkflowOutput.get().getWorkflowId());

      JSONObject jsonObject = new JSONObject();
      jsonObject.put(WORKFLOW_ID, resourceContext.getId());
      jsonObject.put("workflowType",RESOURCE_CONTEXT);
      jsonObject.put("output", resourceContextWorkflowOutput.get().getOutput());
      if (request.isIncludeStepOutput()) {
        jsonObject.put("step-output", resourceContextWorkflowOutput.get().getStepOutput());
      }

      Map<String, Object> riskObj = new HashMap<>();
      riskObj.put("riskValue", resourceContextWorkflowOutput.get().getRisk().getRiskValue());
      riskObj.put("condition", resourceContextWorkflowOutput.get().getRisk().getCondition());
      riskObj.put("name", resourceContextWorkflowOutput.get().getRisk().getName());
      jsonObject.put("risk", riskObj);
      dassanaMap.put(RESOURCE_CONTEXT, jsonObject);
      combinedRisk.setResourceContextRisk(resourceContextWorkflowOutput.get().getRisk());

    }

    messageBody.put(DASSANA_KEY, dassanaMap);

    return messageBody.toString();


  }


}
