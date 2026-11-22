package internal.org.springframework.content.rest.scoped.projection;

import org.springframework.content.fs.store.FileSystemContentStore;
import org.springframework.content.rest.StoreRestResource;
import org.springframework.content.test.support.jpa.fixture.SingleContentJpaLongIdUuidNamedEntity;

import java.util.UUID;

/**
 * @author Vladimir Svyatski
 */
@SuppressWarnings("unused") // classpath / store component scan
@StoreRestResource(path = "projectedContentEntities")
public interface ProjectedContentEntityStore
    extends FileSystemContentStore<SingleContentJpaLongIdUuidNamedEntity, UUID> {
}