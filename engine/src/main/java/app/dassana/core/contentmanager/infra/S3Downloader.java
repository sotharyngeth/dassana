package app.dassana.core.contentmanager.infra;

import static app.dassana.core.contentmanager.ContentManager.DASSANA_MANAGEMENT_BUCKET;

import app.dassana.core.contentmanager.ContentReader;
import app.dassana.core.contentmanager.RemoteWorkflows;
import app.dassana.core.launch.model.Request;
import app.dassana.core.util.StringyThings;
import app.dassana.core.workflow.model.Workflow;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;

@Singleton
public class S3Downloader implements RemoteWorkflows {

  @Inject ContentReader contentReader;
  private final S3Client s3Client;
  String s3Bucket = System.getenv(
      DASSANA_MANAGEMENT_BUCKET); //this is made available via CFT. Checkout "Variables" of DassanaEngine
  // and DassanaApi. In local dev setup, manually set it

  public static final String LAST_UPDATED_CHECK_CACHE = "last-updated-check-cache";

  private final LoadingCache<String, Long> lastUpdatedCheckCache =
      CacheBuilder.newBuilder().expireAfterAccess(Duration.ofSeconds(30)).build(
          new CacheLoader<>() {
            @Override
            public Long load(String key) {
              return getContentLastUpdated();
            }
          });

  public static final String CONTENT_LAST_UPDATED_CACHE_KEY = "content-last-updated";


  private Long getContentLastUpdated() {

    try {
      var headObjectResponse = s3Client.headObject(
          HeadObjectRequest.builder().bucket(s3Bucket)
              .key(CONTENT_LAST_UPDATED_CACHE_KEY)
              .build());
      return headObjectResponse.lastModified().toEpochMilli();
    } catch (NoSuchKeyException noSuchKeyException) {
      return 0L;
    }

  }

  private final LoadingCache<Long, Set<Workflow>> lastUpdatedCache =
      CacheBuilder.newBuilder().maximumSize(100).weakKeys().build(
          new CacheLoader<>() {
            @Override
            public Set<Workflow> load(Long key) {

              final Set<Workflow> workflowSet = new HashSet<>();

              logger.debug("Content is being downloaded from s3 for cache key {}", key);
              ListObjectsV2Request request = ListObjectsV2Request.builder().bucket(s3Bucket).prefix(WORKFLOW_PATH_IN_S3)
                  .build();
              ListObjectsV2Iterable response = s3Client.listObjectsV2Paginator(request);
              for (ListObjectsV2Response page : response) {
                for (S3Object object : page.contents()) {
                  if (object.size() > 0) {
                    try {
                      s3Client
                          .getObject(GetObjectRequest.builder().
                                  bucket(s3Bucket).key(object.key()).build(),
                              (getObjectResponse, abortableInputStream) -> {//handle the file

                                Workflow workflow = contentReader.getWorkflow(
                                    new JSONObject(
                                        StringyThings.getJsonFromYaml(
                                            new String(abortableInputStream.readAllBytes()))));

                                workflow.setPath(
                                    "s3://".concat(DASSANA_MANAGEMENT_BUCKET).concat("/").concat(WORKFLOW_PATH_IN_S3)
                                        .concat(workflow.getId()));

                                workflowSet.remove(workflow);
                                workflowSet.add(workflow);
                                return null;
                              });
                    } catch (S3Exception exception) {//see https://github.com/aws/aws-sdk-java-v2/issues/428
                      if (exception.toBuilder().statusCode() != 304) {
                        throw new RuntimeException(exception);
                      }
                    }
                  }
                }//end iterating all pages
              }//all s3 files have been processed now
              return workflowSet;

            }
          });


  public static final String WORKFLOW_PATH_IN_S3 = "workflows/";

  private static final Logger logger = LoggerFactory.getLogger(S3Downloader.class);


  public S3Downloader(S3Client s3Client) {
    this.s3Client = s3Client;
  }


  @Override
  public Set<Workflow> getWorkflowSet(Request request) throws ExecutionException {

    //this is a relatively expensive operation as we make s3 remote network calls here.
    //so the strategy here is that if isRefreshFromS3() is set, we do not look up value of last modified in
    // lastUpdatedCheckCache. isRefreshFromS3 is set when we are using editor.dassana.io

    Long contentLastUpdated;
    if (request.isRefreshFromS3()) {
      contentLastUpdated = getContentLastUpdated();
    } else {
      contentLastUpdated = lastUpdatedCheckCache.get(LAST_UPDATED_CHECK_CACHE);
    }

    return lastUpdatedCache.get(contentLastUpdated);

  }

  @Override
  public String getWorkflowById(String workflowId) {

    final String[] workflowYaml = {""};
    try {
      s3Client.getObject(
          GetObjectRequest.builder().bucket(System.getenv(DASSANA_MANAGEMENT_BUCKET))
              .key(WORKFLOW_PATH_IN_S3.concat(workflowId))
              .build(), (getObjectResponse, abortableInputStream) -> {
            byte[] readAllBytes = abortableInputStream.readAllBytes();
            workflowYaml[0] = new String(readAllBytes);
            return null;
          });
    } catch (NoSuchKeyException e) {
      return "";
    }

    return workflowYaml[0];
  }
}

