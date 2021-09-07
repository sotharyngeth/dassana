package app.dassana.core.launch;

import static app.dassana.core.launch.ApiHandler.WORKFLOW_PATH;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.utils.IoUtils;

//quick and dirty helper class which simulates invocation of DassanaEngineApi lambda function. We could use SAM local
// proxy but that's quite slow to run and hard to debug. This class is NEVER EVER to be run to process real alerts,
// just use it for testing locally
public class App {

  public static void main(String[] args) throws IOException {

    ApiHandler apiHandler = new ApiHandler();

    if (args.length > 0) {
      System.out.println(handleGet(apiHandler, args[0]));
    } else {
      System.out.println(handleRun(apiHandler));
    }


  }

  private static String handleGet(ApiHandler apiHandler, String workflowId) {

    APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent = new APIGatewayProxyRequestEvent();
    apiGatewayProxyRequestEvent.setIsBase64Encoded(false);
    apiGatewayProxyRequestEvent.setPath(WORKFLOW_PATH);

    apiGatewayProxyRequestEvent.setHttpMethod("GET");
    Map<String, String> queryStringParams = new HashMap<>();
    queryStringParams.put("workflowId", workflowId);
    apiGatewayProxyRequestEvent.setQueryStringParameters(queryStringParams);

    APIGatewayProxyResponseEvent responseEvent = apiHandler.execute(apiGatewayProxyRequestEvent);
    return responseEvent.getBody();


  }


  private static String handleRun(ApiHandler apiHandler) throws IOException {

    String event = IoUtils
        .toUtf8String(Thread.currentThread().getContextClassLoader().getResourceAsStream("TestEvent.json"));
    APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent = new APIGatewayProxyRequestEvent();

    apiGatewayProxyRequestEvent.setBody(event);
    apiGatewayProxyRequestEvent.setIsBase64Encoded(false);

    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("skipGeneralContext", "false");
    queryParams.put("skipPolicyContext", "false");
    queryParams.put("skipPostProcessor", "false");
    queryParams.put("skipS3Upload", "false");

    apiGatewayProxyRequestEvent.setQueryStringParameters(queryParams);
    apiGatewayProxyRequestEvent.setHttpMethod("post");
    apiGatewayProxyRequestEvent.setPath("/run");
    Map<String, String> headers = new HashMap<>();
    headers.put("x-dassana-cache", "true");
    apiGatewayProxyRequestEvent.setHeaders(headers);

    APIGatewayProxyResponseEvent responseEvent = apiHandler.execute(apiGatewayProxyRequestEvent);
    System.out.println(responseEvent.getBody());
    if (responseEvent.getStatusCode() != 200) {
      throw new RuntimeException(responseEvent.getBody());
    } else {
      return responseEvent.getBody();
    }


  }

}
