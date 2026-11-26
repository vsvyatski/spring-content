package it.rest.extensions.content.search.support;

import org.springframework.content.commons.search.Searchable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Collections;

/**
 * FS store fragment for {@link org.springframework.content.commons.search.Searchable}; required by
 * {@code StoreFragmentDetector} (name must be {@code SearchableImpl}). Fulltext calls are mocked via
 * {@code ReflectionService} in {@code ContentSearchRestControllerIT}.
 */
public class SearchableImpl implements Searchable<Object> {

    @Override
    public Iterable<Object> search(String queryString) {
        return Collections.emptyList();
    }

    @Override
    public Page<Object> search(String queryString, Pageable pageable) {
        return new PageImpl<>(Collections.emptyList());
    }
}
