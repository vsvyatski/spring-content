package internal.org.springframework.content.rest.scoped.storeresolver;

import org.springframework.content.jpa.store.JpaContentStore;
import org.springframework.content.test.support.jpa.fixture.SingleContentJpaLongIdMimeEntity;

/**
 * @author Vladimir Svyatski
 */
@SuppressWarnings("unused") // classpath / store component scan
public interface StoreResolverEntityJpaStore extends JpaContentStore<SingleContentJpaLongIdMimeEntity, String> {
}
