package app.dassana.core.launch;

import static app.dassana.core.launch.ApiHandler.API_INCLUDE_ALERT_IN_OUTPUT;
import static app.dassana.core.launch.ApiHandler.API_PARAM_SKIP_GENERAL_CONTEXT;
import static app.dassana.core.launch.ApiHandler.API_PARAM_SKIP_POLICY_CONTEXT;
import static app.dassana.core.launch.ApiHandler.API_PARAM_SKIP_POST_PROCESSOR;
import static app.dassana.core.launch.ApiHandler.API_PARAM_SKIP_S3UPLOAD;
import static app.dassana.core.launch.ApiHandler.VALIDATE_WORKFLOW_PATH;
import static app.dassana.core.launch.ApiHandler.WORKFLOW_PATH;

import app.dassana.core.api.DassanaWorkflowValidationException;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Singleton;
import org.apache.commons.io.IOUtils;

@Singleton
public class Helper {

  public Helper() {
  }

  public String getFileContent(String fileName) throws IOException {
    return IOUtils.toString(Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName),
        Charset.defaultCharset());
  }

  private APIGatewayProxyRequestEvent getApiGatewayProxyRequestEvent(Map<String, String> parameters) {
    APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent = new APIGatewayProxyRequestEvent();
    if (parameters != null && parameters.size() > 0) {
      apiGatewayProxyRequestEvent.setQueryStringParameters(parameters);
    }
    apiGatewayProxyRequestEvent.setIsBase64Encoded(false);
    return apiGatewayProxyRequestEvent;
  }


  public void executeValidateApi(String fileName) throws IOException {

    APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent = getApiGatewayProxyRequestEvent(null);
    apiGatewayProxyRequestEvent.setPath(VALIDATE_WORKFLOW_PATH);
    apiGatewayProxyRequestEvent.setBody(getFileContent(fileName));
    apiGatewayProxyRequestEvent.setHttpMethod("post");

    ApiHandler apiHandler = new ApiHandler();
    APIGatewayProxyResponseEvent responseEvent = apiHandler.execute(apiGatewayProxyRequestEvent);
    if (responseEvent.getStatusCode() != 200) {
      throw new DassanaWorkflowValidationException(responseEvent.getBody());
    }

  }


  public APIGatewayProxyResponseEvent executeRunApiGet(Map<String, String> parameters) {

    APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent = getApiGatewayProxyRequestEvent(parameters);
    apiGatewayProxyRequestEvent.setHttpMethod("get");
    apiGatewayProxyRequestEvent.setPath(WORKFLOW_PATH);
    ApiHandler apiHandler = new ApiHandler();
    return apiHandler.execute(apiGatewayProxyRequestEvent);
  }

  public String executeRunApi(String inputJsonStr, Map<String, String> parameters) {

    APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent = getApiGatewayProxyRequestEvent(parameters);
    apiGatewayProxyRequestEvent.setBody(inputJsonStr);
    apiGatewayProxyRequestEvent.setPath("/run");
    apiGatewayProxyRequestEvent.setHttpMethod("post");
    ApiHandler apiHandler = new ApiHandler();
    APIGatewayProxyResponseEvent responseEvent = apiHandler.execute(apiGatewayProxyRequestEvent);
    return responseEvent.getBody();
  }

  //todo: use builder pattern
  Map<String, String> getQueryParams(boolean includeInputRequest) {

    Map<String, String> queryParams = new HashMap<>();
    queryParams.put(API_PARAM_SKIP_GENERAL_CONTEXT, "false");
    queryParams.put(API_PARAM_SKIP_POLICY_CONTEXT, "false");
    queryParams.put(API_PARAM_SKIP_POST_PROCESSOR, "true");
    queryParams.put(API_PARAM_SKIP_S3UPLOAD, "true");
    if (includeInputRequest) {
      queryParams.put(API_INCLUDE_ALERT_IN_OUTPUT, "true");
    } else {
      queryParams.put(API_INCLUDE_ALERT_IN_OUTPUT, "false");
    }

    return queryParams;

  }


}
