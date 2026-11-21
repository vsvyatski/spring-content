package org.springframework.content.test.support.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * @author Vladimir Svyatski
 */
public interface SingleContentMongoRepository extends MongoRepository<SingleContentMongoDocument, String> {
}