package app.dassana.core.contentmanager.infra;

import app.dassana.core.contentmanager.RemoteContentDownloadApi;
import java.io.File;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Singleton;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;

@Singleton
public class S3Downloader implements RemoteContentDownloadApi {

  private final S3Client s3Client;
  private String s3Bucket;
  public static final String WORKFLOW_PATH_IN_S3="workflows/";

  public S3Downloader(S3Client s3Client) {
    this.s3Client = s3Client;

  }


  private static final Logger logger = LoggerFactory.getLogger(S3Downloader.class);


  @Override
  public Optional<File> downloadContent(Long lastDownloaded) {

    s3Bucket = System.getenv("dassanaBucket");
    Objects.requireNonNull(s3Bucket, "dassanaBucket must be specified as env var");
    //this is made available via CFT. Checkout "Variables" of DassanaEngine
    // and DassanaApi. In local dev setup, manually set it

    //this is the local temp dir where we download content
    String tempDir = System.getProperty("java.io.tmpdir").concat(File.separator);
    AtomicBoolean fileDownloaded = new AtomicBoolean(false);
    ListObjectsV2Request request = ListObjectsV2Request.builder().bucket(s3Bucket).prefix(WORKFLOW_PATH_IN_S3).build();
    ListObjectsV2Iterable response = s3Client.listObjectsV2Paginator(request);
    for (ListObjectsV2Response page : response) {
      for (S3Object object : page.contents()) {
        if (object.size() > 0) {
          try {
            s3Client
                .getObject(GetObjectRequest.builder().
                    bucket(s3Bucket).key(object.key()).
                    ifModifiedSince(Instant.ofEpochMilli(lastDownloaded))
                    .build(), (getObjectResponse, abortableInputStream) -> {//handle the file
                  fileDownloaded.set(true);
                  File file = new File(tempDir.concat(object.key()));
                  file.deleteOnExit();//we do not want the downloaded files to be lying around, it causes debugging
                  // issues when running locally as when you make change to the workflows you want the freshest content
                  // possible and some stale file
                  FileUtils.copyInputStreamToFile(abortableInputStream, file);
                  return null;
                });
          } catch (S3Exception exception) {//see https://github.com/aws/aws-sdk-java-v2/issues/428
            if (exception.toBuilder().statusCode() != 304) {
              throw new RuntimeException(exception);
            }
          }

        }
      }
    }//all s3 files have been downloaded now

    if (fileDownloaded.get()) {
      File workflows = new File(tempDir.concat("workflows"));
      return Optional.of(workflows);
    } else {
      return Optional.empty();
    }
  }
}
