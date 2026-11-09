package internal.org.springframework.content.rest.annotations;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

@Documented
@Component
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.ANNOTATION_TYPE })
@StoreAwareController
public @interface ContentRestController {

}
