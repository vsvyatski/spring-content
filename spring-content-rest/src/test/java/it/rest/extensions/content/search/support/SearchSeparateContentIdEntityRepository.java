package it.rest.extensions.content.search.support;

import org.springframework.content.rest.FulltextEntityLookupQuery;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

@RepositoryRestResource(
    path = "searchSeparateContentIdEntities",
    collectionResourceRel = "searchSeparateContentIdEntities")
public interface SearchSeparateContentIdEntityRepository
    extends CrudRepository<SearchSeparateContentIdEntity, String> {

    @Query("select e from SearchSeparateContentIdEntity e")
    @SuppressWarnings("unused")
    List<SearchSeparateContentIdEntity> randomQueryMethod();

    @FulltextEntityLookupQuery
    @SuppressWarnings("unused")
    List<SearchSeparateContentIdEntity> findAllByContentIdIn(@Param("contentIds") List<String> contentIds);
}