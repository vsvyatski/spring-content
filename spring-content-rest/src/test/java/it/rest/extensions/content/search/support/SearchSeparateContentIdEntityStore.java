package it.rest.extensions.content.search.support;

import org.springframework.content.commons.search.Searchable;
import org.springframework.content.fs.store.FileSystemContentStore;

@SuppressWarnings("unused") // classpath / store component scan
public interface SearchSeparateContentIdEntityStore
    extends FileSystemContentStore<SearchSeparateContentIdEntity, String>, Searchable<String> {
}