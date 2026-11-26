package it.rest.extensions.content.search.support;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract check for the FS {@link org.springframework.content.commons.search.Searchable} test double used by
 * {@code ContentSearchRestControllerIT}.
 *
 * @author Vladimir Svyatski
 */
class SearchableImplTest {

    @Test
    void shouldReturnEmptyIterableForUnpagedSearch() {
        assertThat(new SearchableImpl().search("query")).isEmpty();
    }

    @Test
    void shouldReturnEmptyPageForPagedSearch() {
        var page = new SearchableImpl().search("query", PageRequest.of(0, 10));
        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
    }
}