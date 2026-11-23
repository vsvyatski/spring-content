package it.rest.extensions.content.search.support;

import org.springframework.content.commons.search.Searchable;
import org.springframework.content.fs.store.FileSystemContentStore;

@SuppressWarnings("unused") // classpath / store component scan
public interface CustomSearchReturnTypeEntityStore
    extends FileSystemContentStore<CustomSearchReturnTypeEntity, String>, Searchable<CustomResult> {
}