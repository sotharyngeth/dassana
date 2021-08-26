package app.dassana.core.api;

public class VersionResponse {

  boolean upgradeAvailable = false;
  String installedVersion;
  String latestVersion;
  String message;

  public boolean isUpgradeAvailable() {
    return upgradeAvailable;
  }

  public void setUpgradeAvailable(boolean upgradeAvailable) {
    this.upgradeAvailable = upgradeAvailable;
  }

  public String getInstalledVersion() {
    return installedVersion;
  }

  public void setInstalledVersion(String installedVersion) {
    this.installedVersion = installedVersion;
  }

  public String getLatestVersion() {
    return latestVersion;
  }

  public void setLatestVersion(String latestVersion) {
    this.latestVersion = latestVersion;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }
}
