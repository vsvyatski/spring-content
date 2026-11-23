package it.rest.extensions.content.search.support;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(
    path = "searchSharedContentIdEntities",
    collectionResourceRel = "searchSharedContentIdEntities")
public interface SearchSharedContentIdEntityRepository extends CrudRepository<SearchSharedContentIdEntity, String> {
}