package internal.org.springframework.content.rest.support;

import org.springframework.content.fs.store.FileSystemContentStore;
import org.springframework.content.rest.StoreRestResource;

@StoreRestResource(path = "oneToOneLinkedContentEntities")
public interface OneToOneLinkedContentEntityStore extends FileSystemContentStore<OneToOneLinkedContentEntity, Long> {
}
