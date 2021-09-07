package app.dassana.core.contentmanager;

import app.dassana.core.launch.model.Request;
import app.dassana.core.workflow.model.Workflow;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public interface RemoteWorkflows {

  /**
   * downloads content from remote storage system such as s3 bucket
   */
  Set<Workflow> getWorkflowSet(Request request) throws ExecutionException;

  String getWorkflowById(String workflowId);

}
