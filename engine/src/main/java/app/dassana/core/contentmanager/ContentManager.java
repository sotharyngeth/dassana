package app.dassana.core.contentmanager;

import app.dassana.core.launch.model.Request;
import app.dassana.core.util.StringyThings;
import app.dassana.core.workflow.model.Workflow;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import javax.inject.Singleton;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ContentManager implements ContentManagerApi {

  ContentReader contentReader;

  private final RemoteWorkflows remoteWorkflows;

  public static final String POLICY_CONTEXT = "policy-context";
  public static final String POLICY_CONTEXT_CAT = "category";
  public static final String POLICY_CONTEXT_SUB_CAT = "subcategory";
  public static final String GENERAL_CONTEXT = "general-context";
  public static final String RESOURCE_CONTEXT = "resource-context";
  public static final String NORMALIZE = "normalize";
  public static final String WORKFLOW_ID = "workflowId";
  public static final String DASSANA_MANAGEMENT_BUCKET = "dassanaBucket";

  final Set<Workflow> workflowSet = new HashSet<>();


  private static final Logger logger = LoggerFactory.getLogger(ContentManager.class);


  public ContentManager(RemoteWorkflows contentDownloader, ContentReader contentReader) {
    this.remoteWorkflows = contentDownloader;
    this.contentReader = contentReader;
    embeddedContent.refresh(EMBEDDED_CACHE_KEY);
  }

  public static final String EMBEDDED_CACHE_KEY = "embeddedContent";


  LoadingCache<String, Set<Workflow>> embeddedContent = CacheBuilder.newBuilder().build(
      new CacheLoader<>() {
        @Override
        public Set<Workflow> load(String key) throws Exception {
          return getWorkflowsFromEmbeddedContentDir(new File(Objects.requireNonNull(
              Thread.currentThread().getContextClassLoader().getResource("content/workflows/"),
              "Unable to read embedded content, make sure to run \"mvn clean process-resources\""
                  + " first if you are running it locally ").getFile()));
        }
      });


  public Set<Workflow> getEmbeddedWorkflows() throws ExecutionException {
    return embeddedContent.get(EMBEDDED_CACHE_KEY);
  }


  public String getWorkflowYamlById(String workflowId, Request request) throws Exception {

    for (Workflow workflow : getWorkflowSet(request)) {
      if (workflow.getId().contentEquals(workflowId)) {
        String path = workflow.getPath();

        if (path.startsWith("s3://")) {
          return remoteWorkflows.getWorkflowById(workflowId);
        } else {
          return new
              String(Files.readAllBytes(Paths.get(path)));
        }

      }
    }

    return "";


  }


  public Set<Workflow> getWorkflowsFromEmbeddedContentDir(File dir) {

    File[] files = dir.listFiles();
    if (files != null) {
      for (File file : files) {
        if (file.isDirectory()) {
          getWorkflowsFromEmbeddedContentDir(file);
        } else {
          try {
            String yamlWorkflow = new String(Files.readAllBytes(Paths.get(file.getPath())));
            Workflow workflow =
                contentReader.getWorkflow(
                    new JSONObject(StringyThings.getJsonFromYaml(yamlWorkflow)));
            workflow.setPath(file.getCanonicalPath());
            workflowSet.add(workflow);
          } catch (Exception e) {
            logger.warn("{} will be skipped due to error ", file, e);
          }
        }
      }
    }

    return workflowSet;
  }


  private Set<Workflow> getProvidedWorkFlows(Request request) throws JsonProcessingException {
    Set<Workflow> workflows = new HashSet<>();

    if (request.getAdditionalWorkflowYamls() != null
        && request.getAdditionalWorkflowYamls().size() > 0) {

      List<String> additionalWorkflowYamls = request.getAdditionalWorkflowYamls();
      for (String workflowYamlStr : additionalWorkflowYamls) {
        String jsonFromYaml = StringyThings.getJsonFromYaml(workflowYamlStr);
        Workflow workflow = contentReader.getWorkflow(new JSONObject(jsonFromYaml));
        workflows.add(workflow);
      }
      return workflows;
    } else {
      return new HashSet<>();
    }
  }


  @Override
  public Set<Workflow> getWorkflowSet(Request request) throws Exception {

    var embeddedWorkflows = getEmbeddedWorkflows();
    var customWorkflows = remoteWorkflows.getWorkflowSet(request);
    var providedWorkFlows = getProvidedWorkFlows(request);

    //overwrite embedded workflows with the custom workflows in the remote storage (s3)
    for (var customWorkflow : customWorkflows) {
      embeddedWorkflows.remove(customWorkflow);
      embeddedWorkflows.add(customWorkflow);
    }

    //if the workflows are provided,we use them. This is for the editor.dassana.io use case where we are
    // editing workflows

    if (providedWorkFlows.size() > 0) {
      Set<Workflow> workflowSetToUse = new HashSet<>(embeddedWorkflows);

      for (Workflow providedWorkflow : providedWorkFlows) {
        boolean remove = workflowSetToUse.remove(providedWorkflow);
        if (remove) {
          logger.info("Removing {} workflow from default workflow set", providedWorkflow.getId());
        }
        workflowSetToUse.add(providedWorkflow);
      }
      return workflowSetToUse;

    }

    return embeddedWorkflows;
  }

  @Override
  public Map<String, Workflow> getWorkflowIdToWorkflowMap(Request request) throws Exception {

    if (request.getWorkflowSet() == null || request.getWorkflowSet().isEmpty()) {
      request.setWorkflowSet(getWorkflowSet(request));
    }

    Map<String, Workflow> map = new HashMap<>();
    request.getWorkflowSet().forEach(workflow -> map.put(workflow.getId(), workflow));
    return map;
  }

}
