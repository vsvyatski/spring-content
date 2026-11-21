package org.springframework.content.commons.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.util.ReflectionUtils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ReflectionServiceTest {

    // mocks
    private HelloWorldService service;

    public interface HelloWorldService {
        void helloWorld();
    }

    @Nested
    class ReflectionServiceCases {
        @Nested
        class InvokeMethod {
            @BeforeEach
            void setUp() {
                service = mock(HelloWorldService.class);

                ReflectionService reflectionService = new ReflectionServiceImpl();
                reflectionService.invokeMethod(
                    ReflectionUtils.findMethod(HelloWorldService.class, "helloWorld"), service
                );
            }

            @Test
            void shouldInvokeTheMethod() {
                verify(service).helloWorld();
            }
        }
    }
}
