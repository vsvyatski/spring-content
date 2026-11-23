package it.rest.extensions.content.search.support;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@SuppressWarnings("unused") // classpath / Spring Data repository scan
@RepositoryRestResource(path = "searchNotSearchableEntities")
public interface SearchNotSearchableEntityRepository extends CrudRepository<SearchNotSearchableEntity, String> {
}