package internal.org.springframework.content.jpa.testsupport.repositories;

import internal.org.springframework.content.jpa.testsupport.models.Document;
import org.springframework.data.repository.CrudRepository;

public interface DocumentRepository extends CrudRepository<Document, Long> {
}
