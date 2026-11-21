package internal.org.springframework.content.jpa.support.repositories;

import internal.org.springframework.content.jpa.support.models.Document;
import org.springframework.data.repository.CrudRepository;

@SuppressWarnings("unused") // classpath / store component scan
public interface DocumentRepository extends CrudRepository<Document, Long> {
}
