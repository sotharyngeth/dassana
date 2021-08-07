package app.dassana.actions;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import java.util.Map;
import org.json.JSONObject;
import software.amazon.awssdk.arns.Arn;
import software.amazon.awssdk.arns.ArnResource;
import software.amazon.awssdk.services.s3.S3Client;

public class App implements RequestHandler<Map<String, Object>, Response> {

  public App() {
  }

  @Override
  public Response handleRequest(final Map<String, Object> input, final Context context) {
    JSONObject inputJson = new JSONObject(input);
    Response response = new Response();

    JSONObject finding = inputJson.getJSONObject("detail").getJSONArray("findings").getJSONObject(0);

    response.setAlertId(finding.getString("Id"));
    response.setArn(finding.getJSONArray("Resources").getJSONObject(0).getString("Id"));
    response.setPolicyId(finding.getJSONObject("FindingProviderFields").getJSONArray("Types").getString(0));
    response.setCsp("aws");
    response.setResourceContainer(finding.getString("AwsAccountId"));
    response.setRegion(finding.getJSONArray("Resources").getJSONObject(0).getString("Region"));
    response.setService(Arn.fromString(response.getArn()).service());
    ArnResource resource = Arn.fromString(response.getArn()).resource();
    String resourceType = "";
    if (resource.resourceType().isPresent()) {
      resourceType = resource.resourceType().get();
    }
    response.setResourceType(resourceType);

    String resourceId;

    String[] arnElements = response.getArn().split(":");
    resourceId = arnElements[arnElements.length - 1];

    if (resourceId.contains("/")) {
      String[] split = resourceId.split("/");
      resourceId = split[1];
    }
    response.setResourceId(resourceId);

    return response;
  }
}
