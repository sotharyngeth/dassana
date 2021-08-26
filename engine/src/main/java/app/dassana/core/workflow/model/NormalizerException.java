package app.dassana.core.workflow.model;

public class NormalizerException extends RuntimeException{

  String workflowId;

  public String getWorkflowId() {
    return workflowId;
  }

  public void setWorkflowId(String workflowId) {
    this.workflowId = workflowId;
  }

  public NormalizerException() {
  }

  public NormalizerException(String message) {
    super(message);
  }

  public NormalizerException(String message, Throwable cause) {
    super(message, cause);
  }

  public NormalizerException(Throwable cause) {
    super(cause);
  }

  public NormalizerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
