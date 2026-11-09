package internal.org.springframework.content.jpa.testsupport.stores;

import internal.org.springframework.content.jpa.testsupport.models.Claim;
import org.springframework.content.commons.store.ContentStore;

@SuppressWarnings("unused") // classpath / store component scan
public interface ClaimStore extends ContentStore<Claim, String> {
}
