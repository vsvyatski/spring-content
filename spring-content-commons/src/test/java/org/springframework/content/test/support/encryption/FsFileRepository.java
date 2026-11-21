package org.springframework.content.test.support.encryption;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @author Vladimir Svyatski
 */
@RepositoryRestResource(path = "files")
public interface FsFileRepository extends CrudRepository<FsFile, Long> {
}