package app.dassana.core.contentmanager;

import java.io.File;
import java.util.Optional;

public interface RemoteContentDownloadApi {

  Optional<File> downloadContent(Long lastDownloaded);

}
