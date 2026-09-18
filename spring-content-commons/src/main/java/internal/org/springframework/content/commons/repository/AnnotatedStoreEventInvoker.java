package internal.org.springframework.content.commons.repository;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.content.commons.annotations.*;
import org.springframework.content.commons.store.events.StoreEvent;
import org.springframework.content.commons.store.events.*;
import org.springframework.content.commons.utils.ReflectionService;
import org.springframework.content.commons.utils.ReflectionServiceImpl;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AnnotatedStoreEventInvoker
        implements ApplicationListener<ApplicationEvent>, BeanPostProcessor {

    private static final Log logger = LogFactory.getLog(AnnotatedStoreEventInvoker.class);

    private final MultiValueMap<Class<? extends ApplicationEvent>, EventHandlerMethod> handlerMethods = new LinkedMultiValueMap<>();

    private final ReflectionService reflectionService;

    public AnnotatedStoreEventInvoker() {
        reflectionService = new ReflectionServiceImpl();
    }

    public AnnotatedStoreEventInvoker(ReflectionService reflectionService) {
        this.reflectionService = reflectionService;
    }

    MultiValueMap<Class<? extends ApplicationEvent>, EventHandlerMethod> getHandlers() {
        return handlerMethods;
    }

    @Override
    public Object postProcessBeforeInitialization(@NonNull Object bean, @NonNull String beanName)
            throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName)
            throws BeansException {

        Class<?> beanType = ClassUtils.getUserClass(bean);
        StoreEventHandler typeAnno = AnnotationUtils.findAnnotation(beanType, StoreEventHandler.class);

        if (typeAnno == null) {
            return bean;
        }

        ReflectionUtils.doWithMethods(beanType, new ReflectionUtils.MethodCallback() {

            @Override
            public void doWith(@NonNull Method method)
                    throws IllegalArgumentException {
                findHandler(bean, method, HandleBeforeGetResource.class, BeforeGetResourceEvent.class);
                findHandler(bean, method, HandleAfterGetResource.class, AfterGetResourceEvent.class);
                findHandler(bean, method, HandleBeforeAssociate.class, BeforeAssociateEvent.class);
                findHandler(bean, method, HandleAfterAssociate.class, AfterAssociateEvent.class);
                findHandler(bean, method, HandleBeforeUnassociate.class, BeforeUnassociateEvent.class);
                findHandler(bean, method, HandleAfterUnassociate.class, AfterUnassociateEvent.class);
                findHandler(bean, method, HandleBeforeGetContent.class, BeforeGetContentEvent.class);
                findHandler(bean, method, HandleAfterGetContent.class, AfterGetContentEvent.class);
                findHandler(bean, method, HandleBeforeSetContent.class, BeforeSetContentEvent.class);
                findHandler(bean, method, HandleAfterSetContent.class, AfterSetContentEvent.class);
                findHandler(bean, method, HandleBeforeUnsetContent.class, BeforeUnsetContentEvent.class);
                findHandler(bean, method, HandleAfterUnsetContent.class, AfterUnsetContentEvent.class);
            }

        });

        return bean;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (!(event instanceof StoreEvent)) {
            return;
        }
        Class<? extends ApplicationEvent> eventType = event.getClass();

        if (!handlerMethods.containsKey(eventType)) {
            return;
        }

        for (EventHandlerMethod handlerMethod : handlerMethods.get(eventType)) {

            Object src = event.getSource();

            if ((isStoreEventType(handlerMethod.targetType) &&
                    !ClassUtils.isAssignable(handlerMethod.targetType, event.getClass())) ||
                    (!isStoreEventType(handlerMethod.targetType) &&
                            !ClassUtils.isAssignable(handlerMethod.targetType, src.getClass()))) {
                continue;
            }

            List<Object> parameters = new ArrayList<>();
            if (isStoreEventType(handlerMethod.targetType)) {
                parameters.add(event);
            } else {
                parameters.add(src);
            }

            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Invoking %s handler for %s.",
                        event.getClass().getSimpleName(), event.getSource()));
            }

            reflectionService.invokeMethod(handlerMethod.method, handlerMethod.handler,
                    parameters.toArray());
        }
    }

    private static boolean isStoreEventType(Class<?> type) {
        return ClassUtils.isAssignable(StoreEvent.class, type);
    }

    <H extends Annotation> void findHandler(Object bean, Method method,
                                            Class<H> handler, Class<? extends ApplicationEvent> eventType) {
        H annotation = AnnotationUtils.findAnnotation(method, handler);

        if (annotation == null) {
            return;
        }

        Class<?>[] parameterTypes = method.getParameterTypes();

        if (parameterTypes.length == 0) {
            throw new IllegalStateException(String.format(
                    "Event handler method %s must have a content object argument",
                    method.getName()));
        }

        EventHandlerMethod handlerMethod = new EventHandlerMethod(parameterTypes[0], bean, method);

        logger.debug(
                String.format("Annotated handler method found: {%s}", handlerMethod));

        List<EventHandlerMethod> events = handlerMethods.get(eventType);

        if (events == null) {
            events = new ArrayList<>();
        }

        if (events.isEmpty()) {
            handlerMethods.add(eventType, handlerMethod);
            return;
        }

        events.add(handlerMethod);
        Collections.sort(events);
        handlerMethods.put(eventType, events);
    }

    static class EventHandlerMethod implements Comparable<EventHandlerMethod> {

        final Class<?> targetType;
        final Method method;
        final Object handler;

        private EventHandlerMethod(Class<?> targetType, Object handler, Method method) {

            this.targetType = targetType;
            this.method = method;
            this.handler = handler;

            ReflectionUtils.makeAccessible(this.method);
        }

        /*
         * (non-Javadoc)
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        @Override
        public int compareTo(EventHandlerMethod o) {
            return AnnotationAwareOrderComparator.INSTANCE.compare(this.method, o.method);
        }

        @Override
        public String toString() {
            return String.format(
                    "EventHandlerMethod{ targetType=%s, method=%s, handler=%s }",
                    targetType, method, handler);
        }
    }
}
