package it.rest.extensions.content.search.support;

import org.springframework.content.fs.store.FileSystemContentStore;

@SuppressWarnings("unused") // classpath / store component scan
public interface SearchNotSearchableEntityStore extends FileSystemContentStore<SearchNotSearchableEntity, String> {
}