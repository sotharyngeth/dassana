package app.dassana.core.contentmanager.model;

import java.util.Map;

public class WorkflowProcessingResult {

  Map<String, Exception> workflowFileToExceptionMap;

  public Map<String, Exception> getWorkflowFileToExceptionMap() {
    return workflowFileToExceptionMap;
  }

  public void setWorkflowFileToExceptionMap(Map<String, Exception> workflowFileToExceptionMap) {
    this.workflowFileToExceptionMap = workflowFileToExceptionMap;
  }
}
