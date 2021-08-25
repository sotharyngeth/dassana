package app.dassana.core.contentmanager;

import app.dassana.core.contentmanager.model.SyncResult;
import app.dassana.core.launch.model.Request;
import app.dassana.core.workflow.model.Workflow;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

public interface ContentManagerApi {

  /**
   * One time sync. Useful for debugging/api usage.
   *
   * @return SyncResult which might be helpful for debugging, monitoring etc
   */
  SyncResult syncContent(Long lastSuccessfulSync, Request request);

  Set<Workflow> getWorkflowSet(Request request) throws IOException, Exception;

  Map<String, Workflow> getWorkflowIdToWorkflowMap(Request request) throws Exception;


}
