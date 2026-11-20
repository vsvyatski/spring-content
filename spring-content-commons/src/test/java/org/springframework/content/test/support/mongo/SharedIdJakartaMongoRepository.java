package org.springframework.content.test.support.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * @author Vladimir Svyatski
 */
public interface SharedIdJakartaMongoRepository extends MongoRepository<SharedIdJakartaMongoDocument, String> {
}