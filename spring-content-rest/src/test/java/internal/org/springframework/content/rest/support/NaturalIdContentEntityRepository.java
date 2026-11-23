package internal.org.springframework.content.rest.support;

import org.springframework.data.repository.CrudRepository;

public interface NaturalIdContentEntityRepository extends CrudRepository<NaturalIdContentEntity, Long> {

    NaturalIdContentEntity findByName(String name);
}
