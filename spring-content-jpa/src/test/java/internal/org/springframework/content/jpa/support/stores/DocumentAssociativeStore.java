package internal.org.springframework.content.jpa.support.stores;

import internal.org.springframework.content.jpa.support.models.Document;
import org.springframework.content.commons.store.AssociativeStore;

@SuppressWarnings("unused") // classpath / store component scan
public interface DocumentAssociativeStore extends AssociativeStore<Document, String> {
}
