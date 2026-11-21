package internal.org.springframework.content.gcs.it;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Entity;
import org.springframework.content.gcs.Bucket;
import org.springframework.content.test.support.jpa.DualContentJpaLongIdMimeEntity;

/**
 * GCS IT entity with entity-level {@link Bucket}; commons test-jar cannot depend on this module.
 *
 * @author Vladimir Svyatski
 */
@Entity
@Access(AccessType.FIELD)
public class GcsBucketMimeEntity extends DualContentJpaLongIdMimeEntity {

    @Bucket
    @SuppressWarnings("unused") // read by GCS entity-level @Bucket mapping
    public String bucket = "other-bucket";
}