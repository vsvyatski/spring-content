package internal.org.springframework.content.rest.it.mysql;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import internal.org.springframework.content.rest.it.AbstractRestIT;

@SpringBootTest(classes = Application.class, webEnvironment=WebEnvironment.RANDOM_PORT)
public class MySQLRestIT extends AbstractRestIT {
}
