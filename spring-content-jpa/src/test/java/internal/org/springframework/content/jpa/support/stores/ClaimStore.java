package internal.org.springframework.content.jpa.support.stores;

import internal.org.springframework.content.jpa.support.models.Claim;
import org.springframework.content.commons.store.ContentStore;

@SuppressWarnings("unused") // classpath / store component scan
public interface ClaimStore extends ContentStore<Claim, String> {
}
