package internal.org.springframework.content.rest.support;

import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.event.AbstractRepositoryEventListener;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class EventListenerConfig {

    @Bean
    TestEventListener testListener() {
        return new TestEventListener();
    }

    public static class TestEventListener extends AbstractRepositoryEventListener<Object> {
        private final List<Object> beforeCreate = new ArrayList<>();
        private final List<Object> afterCreate = new ArrayList<>();

        @Override
        protected void onBeforeCreate(@NonNull Object entity) {
            beforeCreate.add(entity);
        }

        @Override
        protected void onAfterCreate(@NonNull Object entity) {
            afterCreate.add(entity);
        }

        public void clear() {
            this.afterCreate.clear();
            this.beforeCreate.clear();
        }

        public List<Object> getBeforeCreate() {
            return beforeCreate;
        }

        public List<Object> getAfterCreate() {
            return afterCreate;
        }
    }
}
