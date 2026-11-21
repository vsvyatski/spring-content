package internal.org.springframework.content.gcs.it;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author Vladimir Svyatski
 */
public interface GcsBucketMimeRepository extends JpaRepository<GcsBucketMimeEntity, Long> {
}