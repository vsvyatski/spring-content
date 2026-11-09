package internal.org.springframework.content.jpa.testsupport.repositories;

import internal.org.springframework.content.jpa.testsupport.models.Claim;
import org.springframework.data.repository.CrudRepository;

@SuppressWarnings("unused") // classpath / store component scan
public interface ClaimRepository extends CrudRepository<Claim, String> {
}
