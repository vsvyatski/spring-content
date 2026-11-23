package internal.org.springframework.content.rest.scoped.store.resolver;

import org.springframework.content.jpa.store.JpaContentStore;
import org.springframework.content.test.support.jpa.fixture.SingleContentJpaLongIdMimeEntity;

/**
 * @author Vladimir Svyatski
 */
public interface StoreResolverEntityJpaStore extends JpaContentStore<SingleContentJpaLongIdMimeEntity, String> {
}
