package app.dassana.core.launch;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.IOUtils;

public class App {

  public static void main(String[] args) throws IOException {

    ApiHandler apiHandler = new ApiHandler();
    String event = IOUtils
        .toString(Thread.currentThread().getContextClassLoader().getResourceAsStream("TestEvent.json"),
            Charset.defaultCharset());
    APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent = new APIGatewayProxyRequestEvent();

    apiGatewayProxyRequestEvent.setBody(event);
    apiGatewayProxyRequestEvent.setIsBase64Encoded(false);

    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("skipResourcePrioritization", "false");
    queryParams.put("skipResourceContextualization", "false");
    queryParams.put("skipPostProcessor", "false");
    queryParams.put("S3Downloader","true");

    apiGatewayProxyRequestEvent.setQueryStringParameters(queryParams);

    APIGatewayProxyResponseEvent responseEvent = apiHandler.execute(apiGatewayProxyRequestEvent);
    if (responseEvent.getStatusCode() != 200) {
      throw new RuntimeException(responseEvent.getBody());
    }else {
      System.exit(0);
    }
  }

}
