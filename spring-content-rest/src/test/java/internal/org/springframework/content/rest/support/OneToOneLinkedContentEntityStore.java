package internal.org.springframework.content.rest.support;

import org.springframework.content.fs.store.FileSystemContentStore;
import org.springframework.content.rest.StoreRestResource;

@SuppressWarnings("unused") // classpath / store component scan
@StoreRestResource(path = "oneToOneLinkedContentEntities")
public interface OneToOneLinkedContentEntityStore extends FileSystemContentStore<OneToOneLinkedContentEntity, Long> {
}
