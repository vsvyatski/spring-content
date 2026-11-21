package internal.org.springframework.content.rest.support;

import org.springframework.content.fs.store.FileSystemContentStore;

@SuppressWarnings("unused") // classpath / store component scan
public interface OneToOneLinkedContentEntityStore extends FileSystemContentStore<OneToOneLinkedContentEntity, Long> {
}
