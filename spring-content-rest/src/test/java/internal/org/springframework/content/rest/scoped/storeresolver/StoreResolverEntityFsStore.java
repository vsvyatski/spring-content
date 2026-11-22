package internal.org.springframework.content.rest.scoped.storeresolver;

import org.springframework.content.fs.store.FileSystemContentStore;
import org.springframework.content.rest.StoreRestResource;
import org.springframework.content.test.support.jpa.fixture.SingleContentJpaLongIdMimeEntity;

/**
 * @author Vladimir Svyatski
 */
@SuppressWarnings("unused") // classpath / store component scan
@StoreRestResource(path = "storeResolverEntities")
public interface StoreResolverEntityFsStore extends FileSystemContentStore<SingleContentJpaLongIdMimeEntity, String> {
}
