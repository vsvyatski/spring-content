package internal.org.springframework.content.jpa.support.repositories;

import internal.org.springframework.content.jpa.support.models.Claim;
import org.springframework.data.repository.CrudRepository;

@SuppressWarnings("unused") // classpath / store component scan
public interface ClaimRepository extends CrudRepository<Claim, String> {
}
