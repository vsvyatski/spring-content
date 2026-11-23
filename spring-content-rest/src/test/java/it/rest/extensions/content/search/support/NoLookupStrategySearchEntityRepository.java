package it.rest.extensions.content.search.support;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(
    path = "noLookupStrategySearchEntities",
    collectionResourceRel = "noLookupStrategySearchEntities")
public interface NoLookupStrategySearchEntityRepository extends CrudRepository<NoLookupStrategySearchEntity, String> {
}