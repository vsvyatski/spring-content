package org.springframework.content.commons.search;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface Searchable<T> {

    Iterable<T> search(String queryString);

    Page<T> search(String queryString, Pageable pageable);
}
