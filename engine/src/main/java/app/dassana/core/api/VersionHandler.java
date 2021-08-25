package app.dassana.core.api;

import com.vdurmont.semver4j.Semver;
import java.io.IOException;
import java.nio.charset.Charset;
import javax.inject.Singleton;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;

@Singleton
public class VersionHandler {

  CloseableHttpClient httpClient = HttpClients.createDefault();


  public VersionResponse getVersionResponse() throws IOException {

    String latestVersionJsonResponse = getLatestVersion();
    String latestVersion = new JSONObject(latestVersionJsonResponse).getString("tag_name");
    String currentVersion = System.getenv().get("version");

    VersionResponse versionResponse = new VersionResponse();
    versionResponse.setInstalledVersion(currentVersion);

    Semver installed = new Semver(latestVersion);
    Semver latest = new Semver(latestVersion);

    if (latest.isGreaterThan(installed)) {
      versionResponse.setUpgradeAvailable(true);
      versionResponse.setMessage("Hey there! An upgrade to Dassana is available. Please visit https://docs.dassana"
          + ".io/docs/guides/ops/updating-dassana for instructions");
    } else {
      versionResponse.setUpgradeAvailable(false);
    }
    return versionResponse;


  }


  private String getLatestVersion() throws IOException {
    HttpGet request = new HttpGet("https://api.github.com/repos/dassana-io/dassana/releases/latest");
    try (CloseableHttpResponse response = httpClient.execute(request)) {
      if (response.getStatusLine().getStatusCode() == 200) {
        return IOUtils.toString(response.getEntity().getContent(), Charset.defaultCharset());
      } else {
        throw new RuntimeException(
            "Github API failed :( with response code ".concat(response.getStatusLine().getReasonPhrase()));
      }

    }
  }

}
