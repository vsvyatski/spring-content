package internal.org.springframework.content.rest.support;

import org.springframework.data.repository.CrudRepository;

@SuppressWarnings("unused") // classpath / store component scan
public interface TestEntity7Repository extends CrudRepository<TestEntity7, Long> {

    TestEntity7 findByName(String name);
}
