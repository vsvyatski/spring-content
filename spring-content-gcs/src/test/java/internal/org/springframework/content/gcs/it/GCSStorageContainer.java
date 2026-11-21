package internal.org.springframework.content.gcs.it;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.contrib.nio.testing.LocalStorageHelper;

/**
 * Shared in-memory GCS {@link Storage} for module integration tests (no Docker).
 *
 * @author Vladimir Svyatski
 */
public final class GCSStorageContainer {

    private static final Storage STORAGE = LocalStorageHelper.getOptions().getService();

    private GCSStorageContainer() {
    }

    public static Storage getStorage() {
        return STORAGE;
    }
}