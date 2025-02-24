package app.dassana.resourceid.sh;

import org.json.JSONArray;
import org.json.JSONObject;
import software.amazon.awssdk.arns.Arn;
import software.amazon.awssdk.arns.ArnResource;
import software.amazon.awssdk.utils.StringUtils;

public class Normalize {

  private final String jsonAlertData;

  public Normalize(String jsonAlertData) {
    this.jsonAlertData = jsonAlertData;
  }

  public NormalizationResult normalize() {

    JSONObject jsonObject = new JSONObject(jsonAlertData);
    JSONObject detailJsonObject = jsonObject.getJSONObject("detail");
    JSONArray findingsJsonArray = detailJsonObject.getJSONArray("findings");
    JSONObject finding = findingsJsonArray.getJSONObject(0);

    String possiblePolicyId = finding.getJSONObject("ProductFields").optString("RelatedAWSResources:0/name");
    String policyId = finding.getJSONObject("ProductFields").getString("StandardsControlArn");
    String[] split = policyId.split(":");
    policyId = split[5];

    if (StringUtils.isNotBlank(possiblePolicyId)) {
      policyId = possiblePolicyId;
    }

    String resourceArn = finding.getJSONObject("ProductFields").getString("Resources:0/Id");

    String findingId = finding.getString("Id");

    Arn arn = Arn.fromString(resourceArn);

    ArnResource resource = arn.resource();

    String resourceType = "";
    String resourceId = "";
    if (resource.resourceType().isPresent() && StringUtils.isNotBlank(resource.resourceType().get())) {
      resourceType = resource.resourceType().get();
      resourceId = resource.resource();
    } else {

      String[] arnElements = resourceArn.split(":");
      String resourceInfo = arnElements[5];


      if (resourceInfo.contains("/")) {
        String[] resourceSplit = resourceInfo.split("/");
        resourceType = resourceSplit[1];
        resourceId = resourceSplit[2];
      } else {
        resourceId = resourceInfo;
      }

    }

    NormalizationResult normalizationResult = new NormalizationResult("aws", resourceType, resourceId, findingId);
    normalizationResult.setArn(resourceArn);

    normalizationResult.setPolicyId(policyId);

    if (arn.region().isPresent() && StringUtils.isNotBlank(arn.region().get())) {
      normalizationResult.setRegion(arn.region().get());
    } else {//many a times ARNs do haven't have region e.g. s3 bucket, resources etc so we rely on what the finding
      // value is
      JSONArray resources = findingsJsonArray.getJSONObject(0).optJSONArray("Resources");
      if (resources != null) {
        String region = resources.getJSONObject(0).getString("Region");
        normalizationResult.setRegion(region);
      }

    }

    normalizationResult.setService(arn.service());

    if (arn.accountId().isPresent() && StringUtils.isNotBlank(arn.accountId().get())) {
      normalizationResult.setResourceContainer(arn.accountId().get());
    } else {
      JSONArray resources = findingsJsonArray.getJSONObject(0).optJSONArray("Resources");
      if (resources != null) {
        String awsAccountId = findingsJsonArray.getJSONObject(0).optString("AwsAccountId");
        normalizationResult.setResourceContainer(awsAccountId);
      }
    }

    normalizationResult.setArn(resourceArn);
    return normalizationResult;

  }


}
