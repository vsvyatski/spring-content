package org.springframework.content.commons.search;

import java.util.Map;

public interface FilterQueryProvider {

    Map<String, Object> filterQueries(Class<?> entity);
}