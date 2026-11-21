package internal.org.springframework.content.rest.support;

import org.springframework.data.repository.CrudRepository;

@SuppressWarnings("unused") // classpath / store component scan
public interface NaturalIdContentEntityRepository extends CrudRepository<NaturalIdContentEntity, Long> {

    NaturalIdContentEntity findByName(String name);
}
