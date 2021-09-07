package app.dassana.core.launch;

import static app.dassana.core.contentmanager.ContentManager.DASSANA_MANAGEMENT_BUCKET;
import static app.dassana.core.contentmanager.ContentManager.GENERAL_CONTEXT;
import static app.dassana.core.contentmanager.ContentManager.NORMALIZE;
import static app.dassana.core.contentmanager.ContentManager.POLICY_CONTEXT;
import static app.dassana.core.contentmanager.ContentManager.RESOURCE_CONTEXT;
import static app.dassana.core.contentmanager.ContentManager.WORKFLOW_ID;
import static app.dassana.core.contentmanager.infra.S3Downloader.CONTENT_LAST_UPDATED_CACHE_KEY;
import static app.dassana.core.contentmanager.infra.S3Downloader.WORKFLOW_PATH_IN_S3;
import static app.dassana.core.workflow.processor.Decorator.DASSANA_KEY;

import app.dassana.core.api.DassanaWorkflowValidationException;
import app.dassana.core.api.PingHandler;
import app.dassana.core.api.VersionHandler;
import app.dassana.core.api.WorkflowValidator;
import app.dassana.core.contentmanager.ContentManager;
import app.dassana.core.contentmanager.ContentReader;
import app.dassana.core.launch.model.Message;
import app.dassana.core.launch.model.ProcessingResponse;
import app.dassana.core.launch.model.Request;
import app.dassana.core.launch.model.WorkflowNotFundException;
import app.dassana.core.launch.model.severity;
import app.dassana.core.normalize.model.NormalizerWorkflow;
import app.dassana.core.policycontext.model.PolicyContext;
import app.dassana.core.resource.model.GeneralContext;
import app.dassana.core.resource.model.ResourceContext;
import app.dassana.core.rule.RuleMatch;
import app.dassana.core.util.JsonyThings;
import app.dassana.core.util.StringyThings;
import app.dassana.core.workflow.WorkflowRunner;
import app.dassana.core.workflow.model.NormalizerException;
import app.dassana.core.workflow.model.Workflow;
import app.dassana.core.workflow.model.WorkflowOutputWithRisk;
import app.dassana.core.workflow.processor.RequestProcessor;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.Gson;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.util.StringUtils;
import io.micronaut.function.aws.MicronautRequestHandler;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * todo: this class has become quite big, needs to be refactored
 * <p>
 * <p>
 * Note that there is only one api handler which is receiving ALL api requests instead of different api handlers
 * handling different api requests. This is by design and it might change in future. The reason is that when you create
 * a lambda function to receive a request from api gateway, a lot of resources and wiring needs to be done- you have a
 * cloudwatch log group, lambda resource policy etc to be setup. Given that we have large number of functions to begin
 * with and more functions (actions) will be available in future, we are choosing not to create a function per api but
 * rather handle api routing within this function.In future, we will have more APIs so even this approach isn't optimal
 * either. But for now, complexity in this class is manageable- there are only a few api routes- /run, /validate and
 * /ping
 */
@Introspected
public class ApiHandler extends
    MicronautRequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  @Inject private RequestProcessor requestProcessor;
  @Inject private Gson gson;
  @Inject private RuleMatch ruleMatch;
  @Inject private PingHandler pingHandler;
  @Inject private S3Client s3Client;
  @Inject private WorkflowValidator workflowValidator;
  @Inject private WorkflowRunner workflowRunner;
  @Inject private VersionHandler versionHandler;
  @Inject ContentReader contentReader;


  @Inject ContentManager contentManager; //todo: not a good idea to inject an implementation

  private static final Logger logger = LoggerFactory.getLogger(ApiHandler.class);

  public static final String API_PARAM_SKIP_GENERAL_CONTEXT = "skipGeneralContext";
  public static final String API_PARAM_SKIP_POLICY_CONTEXT = "skipPolicyContext";
  public static final String API_PARAM_SKIP_S3UPLOAD = "skipS3Upload";
  public static final String API_PARAM_SKIP_POST_PROCESSOR = "skipPostProcessor";
  public static final String API_INCLUDE_ALERT_IN_OUTPUT = "includeAlert";
  public static final String API_INCLUDE_STEP_OUTPUT = "includeStepOutput";

  public static final String MISSING_NORMALIZATION_MSG = "Dassana couldn't normalize the alert you sent, please verify "
      + "normalizers filter config";

  public static final String WORKFLOW_PATH = "/workflows";
  public static final String VALIDATE_WORKFLOW_PATH = WORKFLOW_PATH.concat("/validate");


  public ApiHandler() {
  }

  String handleGet(Request request, String workFlowId) throws Exception {
    String workflowYamlById = contentManager.getWorkflowYamlById(workFlowId, request);
    if (StringUtils.isEmpty(workflowYamlById)) {
      throw new WorkflowNotFundException(String.format("Workflow %s not found", workFlowId));
    }
    return workflowYamlById;

  }

  String handleRun(Request request) throws Exception {

    if (StringUtils.isNotEmpty(request.getWorkflowId())) {
      Map<String, Workflow> workflowIdToWorkflowMap = contentManager.getWorkflowIdToWorkflowMap(request);

      if (workflowIdToWorkflowMap.containsKey(request.getWorkflowId())) {
        Workflow workflow = workflowIdToWorkflowMap.get(request.getWorkflowId());
        Optional<WorkflowOutputWithRisk> workflowOutputWithRisk = Optional.empty();
        //todo: generify this
        if (workflow.getClass().getName().contentEquals(NormalizerWorkflow.class.getName())) {
          workflowOutputWithRisk = workflowRunner
              .runWorkFlow(NormalizerWorkflow.class, request, request.getInputJson());
        } else if (workflow.getClass().getName().contentEquals(GeneralContext.class.getName())) {
          workflowOutputWithRisk = workflowRunner.runWorkFlow(GeneralContext.class, request,
              request.getInputJson());
        } else if (workflow.getClass().getName().contentEquals(ResourceContext.class.getName())) {
          workflowOutputWithRisk = workflowRunner.runWorkFlow(ResourceContext.class, request,
              request.getInputJson());
        } else if (workflow.getClass().getName().contentEquals(PolicyContext.class.getName())) {
          workflowOutputWithRisk = workflowRunner.runWorkFlow(PolicyContext.class, request,
              request.getInputJson());
        }

        if (workflowOutputWithRisk.isPresent()) {
          WorkflowOutputWithRisk outputWithRisk = workflowOutputWithRisk.get();
          outputWithRisk.setStepOutput(outputWithRisk.getStepOutput());
          outputWithRisk.setWorkflowId(outputWithRisk.getWorkflowId());
          return gson.toJson(outputWithRisk);
        } else {//this shouldn't happen but still to be safe..
          return "{}";
        }

      } else {
        throw new WorkflowNotFundException(String.format("Sorry, the workflow %s was not found",
            request.getWorkflowId()));
      }

    }

    ProcessingResponse processingResponse = requestProcessor.processRequest(request);
    String decoratedJson = processingResponse.getDecoratedJson();
    return handleHints(decoratedJson, request);
  }


  @Override
  public APIGatewayProxyResponseEvent execute(APIGatewayProxyRequestEvent input) {
    APIGatewayProxyResponseEvent gatewayProxyResponseEvent = new APIGatewayProxyResponseEvent();
    gatewayProxyResponseEvent.setHeaders(getHeaders());

    try {
      String inputBody;
      if (input.getIsBase64Encoded()) {
        inputBody = new String(Base64.getDecoder().decode(input.getBody()));
      } else {
        inputBody = input.getBody();
      }

      if (input.getPath().startsWith("/run") && input.getHttpMethod().toLowerCase().contentEquals("post")) {
        JsonyThings.throwExceptionIfNotValidJsonObj(inputBody);
        String handleRun = handleRun(
            getRequestFromQueryParam(input.getQueryStringParameters(), inputBody, input.getHeaders()));
        gatewayProxyResponseEvent.setBody(handleRun);
      } else if (input.getPath().contentEquals(WORKFLOW_PATH) && input.getHttpMethod().toLowerCase()
          .contentEquals("post")) {
        handleSaveToS3(input.getBody());

      } else if (input.getPath().contentEquals(WORKFLOW_PATH) && input.getHttpMethod().toLowerCase().contentEquals(
          "get")) {
        try {
          String response = handleGet(
              getRequestFromQueryParam(input.getQueryStringParameters(), inputBody, input.getHeaders()),
              input.getQueryStringParameters().get(WORKFLOW_ID));
          gatewayProxyResponseEvent.setBody(response);
          gatewayProxyResponseEvent.getHeaders().put("Content-type", "application/x-yaml");

        } catch (WorkflowNotFundException e) {
          gatewayProxyResponseEvent
              .setBody(String.format("Workflow %s not found", input.getQueryStringParameters().get(WORKFLOW_ID)));
          gatewayProxyResponseEvent.setStatusCode(404);
          return gatewayProxyResponseEvent;
        }
      } else if (input.getPath().contentEquals("/ping")) {
        gatewayProxyResponseEvent.setBody(gson.toJson(pingHandler.getPingResponse()));
      } else if (input.getPath().contentEquals("/version")) {
        gatewayProxyResponseEvent.setBody(gson.toJson(versionHandler.getVersionResponse()));
      } else if (input.getPath().contentEquals(VALIDATE_WORKFLOW_PATH) && input.getHttpMethod().toLowerCase()
          .contentEquals("post")) {
        try {
          workflowValidator.handleValidate(StringyThings.getJsonFromYaml(inputBody));
        } catch (Exception e) {
          if (e instanceof DassanaWorkflowValidationException) {
            List<String> issues = ((DassanaWorkflowValidationException) e).getIssues();
            gatewayProxyResponseEvent.setBody(gson.toJson(issues));
          } else {
            gatewayProxyResponseEvent.setBody(e.getMessage());
          }

          gatewayProxyResponseEvent.setStatusCode(400);
          return gatewayProxyResponseEvent;
        }
      } else {
        throw new RuntimeException("Sorry, I can't handle path ".concat(input.getPath()));
      }

      gatewayProxyResponseEvent.setStatusCode(200);
    } catch (NormalizerException exception) {

      String message = String.format("Sorry but the normalizer %s threw error %s",
          exception.getWorkflowId(), exception.getMessage());
      gatewayProxyResponseEvent.setBody(gson.toJson(message));
      gatewayProxyResponseEvent.setStatusCode(400);
    } catch (Exception e) {
      StringWriter sw = new StringWriter();
      e.printStackTrace(new PrintWriter(sw));
      gatewayProxyResponseEvent.setBody(sw.toString());
      gatewayProxyResponseEvent.setStatusCode(500);
    }
    return gatewayProxyResponseEvent;
  }

  private void handleSaveToS3(String body) throws JsonProcessingException {
    String dassanaBucket = System.getenv().get(DASSANA_MANAGEMENT_BUCKET);
    Workflow workflow = contentReader.getWorkflow(new JSONObject(StringyThings.getJsonFromYaml(body)));
    String key = WORKFLOW_PATH_IN_S3.concat(workflow.getId());
    PutObjectRequest putObjectRequest = PutObjectRequest.builder().bucket(dassanaBucket).key(key).build();
    s3Client.putObject(putObjectRequest, RequestBody.fromString(body, Charset.defaultCharset()));
    s3Client.putObject(
        PutObjectRequest.builder().bucket(dassanaBucket).key(CONTENT_LAST_UPDATED_CACHE_KEY)
            .build(), RequestBody.empty());

  }

  private Map<String, String> getHeaders() {
    Map<String, String> headers = new HashMap<>();
    headers.put("Access-Control-Allow-Headers", "x-api-key , Content-Type, x-dassana-cache");
    headers.put("Access-Control-Allow-Origin", "*");
    headers.put("Access-Control-Allow-Methods", "OPTIONS,POST,GET");
    headers.put("content-type", "application/json");
    return headers;

  }

  private String handleHints(String json, Request request) throws JsonProcessingException {

    JSONObject jsonObject = new JSONObject(json);
    JSONObject dassana = jsonObject.optJSONObject(DASSANA_KEY);

    if (dassana == null || !dassana.has(NORMALIZE)) {
      Message message = new Message();
      message.setSeverity(severity.WARN);
      message.setMsg(MISSING_NORMALIZATION_MSG);
      jsonObject.put(DASSANA_KEY, new JSONObject(gson.toJson(message)));
    } else {//this means that normalization did occur

      String[] workflowKeys = {GENERAL_CONTEXT, RESOURCE_CONTEXT, POLICY_CONTEXT};

      for (String workflowKey : workflowKeys) {
        JSONObject workflowResponse = dassana.optJSONObject(workflowKey);
        if (workflowResponse == null) {
          Message message = new Message();
          message.setSeverity(severity.INFO);
          message.setMsg(String.format("Sorry, but no %s workflow ran for the given alert. Please check filter config",
              workflowKey));
          jsonObject.getJSONObject(DASSANA_KEY).put(workflowKey, new JSONObject(gson.toJson(message)));
        } else {
          //check if the expected workflow did run or not
          if (request.getAdditionalWorkflowYamls() != null && request.getAdditionalWorkflowYamls().size() > 0) {

            for (String workflowYamlStr : request.getAdditionalWorkflowYamls()) {
              String workflowJson = StringyThings.getJsonFromYaml(workflowYamlStr);
              Workflow workflow = contentReader.getWorkflow(new JSONObject(workflowJson));
              String workflowType = workflowResponse.getString("workflowType");
              if (workflowType.contentEquals(workflow.getType())) {
                if (!workflow.getId().contentEquals(workflowResponse.getString(WORKFLOW_ID))) {
                  jsonObject.getJSONObject(DASSANA_KEY).put(workflowKey,
                      new JSONObject(gson.toJson(String.format("the output is from workflow id %s which doesn't "
                              + "match the workflowId provided, please check your filter config",
                          workflowResponse.getString(WORKFLOW_ID)))));
                }
              }


            }

          }

        }
      }


    }

    //we return the dassana only ONLY when we are asked to not include the original alert
    if (!request.isIncludeAlertInOutput()) {
      if (jsonObject.optJSONObject(DASSANA_KEY) != null) {
        return jsonObject.getJSONObject(DASSANA_KEY).toString();
      }
    }

    return jsonObject.toString();
  }

  /**
   * DO NOT put anything cloud implementation specific in this method i.e. do not have any aws/gcp/azure specific
   * constructs in this method as we intend to use this method as abstracted method which can be called by anyone who
   * speaks http layer. As you can see, there is no aws api gateway specific parameters.
   *
   * @param parameters   query params
   * @param inputJsonStr input to process
   * @return request obj which the engine will run
   */
  public Request getRequestFromQueryParam(Map<String, String> parameters, String inputJsonStr,
      Map<String, String> headers) {

    if (parameters == null) {
      parameters = new HashMap<>();
    }
    if (headers == null) {
      headers = new HashMap<>();
    }

    boolean skipGeneralContext = parameters
        .getOrDefault(API_PARAM_SKIP_GENERAL_CONTEXT, "false").contentEquals("true");

    boolean skipPolicyContext = parameters
        .getOrDefault(API_PARAM_SKIP_POLICY_CONTEXT, "false").contentEquals("true");

    boolean skipPostProcessor = parameters
        .getOrDefault(API_PARAM_SKIP_POST_PROCESSOR, "false").contentEquals("true");

    String cache = headers.getOrDefault("x-dassana-cache", "false");

    String workflowId = parameters.getOrDefault(WORKFLOW_ID, "");

    boolean refreshFromS3 = Boolean.parseBoolean(cache);
    Request request = new Request(inputJsonStr);

    request.setWorkflowId(workflowId);

    request
        .setIncludeAlertInOutput(Boolean.parseBoolean(parameters.getOrDefault(API_INCLUDE_ALERT_IN_OUTPUT, "false")));

    request.setIncludeStepOutput(Boolean.parseBoolean(parameters.getOrDefault(API_INCLUDE_STEP_OUTPUT, "false")));

    if (StringUtils.isNotEmpty(inputJsonStr)) {
      JSONObject inputJsonObj = new JSONObject(inputJsonStr);
      JSONArray workflows = inputJsonObj.optJSONArray("workflows");
      if (workflows != null && workflows.length() > 0) {
        List<String> workflowStr = new LinkedList<>();
        for (int i = 0; i < workflows.length(); i++) {
          workflowStr.add(workflows.getString(i));
        }
        request.setAdditionalWorkflowYamls(workflowStr);
      }

    }

    request.setRefreshFromS3(refreshFromS3);

    request.setQueueProcessing(false);
    request.setSkipPostProcessor(skipPostProcessor);
    request.setSkipGeneralContext(skipGeneralContext);
    request.setSkipPolicyContext(skipPolicyContext);
    return request;
  }


}
