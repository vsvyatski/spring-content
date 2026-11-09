package internal.org.springframework.content.rest.support;

import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

@SuppressWarnings("unused") // classpath / store component scan
public interface TestEntity6Repository extends CrudRepository<TestEntity6, UUID> {
}
