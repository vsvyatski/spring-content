package internal.org.springframework.content.gcs.it;

import com.google.cloud.storage.Storage;
import internal.org.springframework.content.gcs.it.support.InMemoryStorage;

/**
 * Shared in-memory GCS {@link Storage} for module integration tests (no Docker).
 *
 * @author Vladimir Svyatski
 */
public final class GCSStorageContainer {

    private static final Storage STORAGE = InMemoryStorage.getOptions().getService();

    private GCSStorageContainer() {
    }

    public static Storage getStorage() {
        return STORAGE;
    }
}
