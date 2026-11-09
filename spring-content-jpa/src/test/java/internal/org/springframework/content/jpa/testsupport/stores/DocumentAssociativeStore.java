package internal.org.springframework.content.jpa.testsupport.stores;

import internal.org.springframework.content.jpa.testsupport.models.Document;
import org.springframework.content.commons.store.AssociativeStore;

@SuppressWarnings("unused") // classpath / store component scan
public interface DocumentAssociativeStore extends AssociativeStore<Document, String> {
}
