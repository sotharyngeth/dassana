package app.dassana.core.launch.model;

import app.dassana.core.workflow.model.Workflow;
import java.util.List;
import java.util.Set;

public class Request {

  private final String inputJson;


  Set<Workflow> workflowSet;

  public Set<Workflow> getWorkflowSet() {
    return workflowSet;
  }

  public void setWorkflowSet(Set<Workflow> workflowSet) {
    this.workflowSet = workflowSet;
  }

  boolean includeStepOutput;
  boolean includeAlertInOutput;

  //content management
  private boolean skipGeneralContext;
  private boolean skipResourceContext;
  private boolean skipPolicyContext;
  private boolean refreshFromS3;
  private List<String> additionalWorkflowYamls;

  private String workflowId; //if specified, only this workflow will run

  //engine specific
  private boolean queueProcessing;
  private boolean skipPostProcessor;

  public Request(String inputJson) {
    this.inputJson = inputJson;
  }

  public boolean isIncludeStepOutput() {
    return includeStepOutput;
  }

  public boolean isIncludeAlertInOutput() {
    return includeAlertInOutput;
  }

  public void setIncludeAlertInOutput(boolean includeAlertInOutput) {
    this.includeAlertInOutput = includeAlertInOutput;
  }

  public void setIncludeStepOutput(boolean includeStepOutput) {
    this.includeStepOutput = includeStepOutput;
  }

  public String getWorkflowId() {
    return workflowId;
  }

  public void setWorkflowId(String workflowId) {
    this.workflowId = workflowId;
  }

  public boolean isRefreshFromS3() {
    return refreshFromS3;
  }

  public List<String> getAdditionalWorkflowYamls() {
    return additionalWorkflowYamls;
  }

  public void setAdditionalWorkflowYamls(List<String> additionalWorkflowYamls) {
    this.additionalWorkflowYamls = additionalWorkflowYamls;
  }

  public void setRefreshFromS3(boolean refreshFromS3) {
    this.refreshFromS3 = refreshFromS3;
  }


  public boolean isSkipPolicyContext() {
    return skipPolicyContext;
  }

  public void setSkipPolicyContext(boolean skipPolicyContext) {
    this.skipPolicyContext = skipPolicyContext;
  }

  public boolean isSkipGeneralContext() {
    return skipGeneralContext;
  }

  public void setSkipGeneralContext(boolean skipGeneralContext) {
    this.skipGeneralContext = skipGeneralContext;
  }

  public String getInputJson() {
    return inputJson;
  }


  public boolean isQueueProcessing() {
    return queueProcessing;
  }

  public void setQueueProcessing(boolean queueProcessing) {
    this.queueProcessing = queueProcessing;
  }

  public boolean isSkipPostProcessor() {
    return skipPostProcessor;
  }

  public void setSkipPostProcessor(boolean skipPostProcessor) {
    this.skipPostProcessor = skipPostProcessor;
  }

  public boolean isSkipResourceContext() {
    return skipResourceContext;
  }

  public void setSkipResourceContext(boolean skipResourceContext) {
    this.skipResourceContext = skipResourceContext;
  }
}
