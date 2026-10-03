package org.springframework.content.commons.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.springframework.content.commons.utils.ReflectionService;
import org.springframework.content.commons.utils.ReflectionServiceImpl;
import org.springframework.util.ReflectionUtils;

public class ReflectionServiceTest {

	private ReflectionService reflectionService;

	// mocks
	private HelloWorldService service;

    @Nested
    class ReflectionServiceCases {
        @Nested
        class InvokeMethod {
            @BeforeEach
            void setUp() {
                service = mock(HelloWorldService.class);

                reflectionService = new ReflectionServiceImpl();
                reflectionService.invokeMethod(ReflectionUtils
                		.findMethod(HelloWorldService.class, "helloWorld"), service,
                		new Object[] {});
            }

            @Test
            void shouldInvokeTheMethod() {
                verify(service).helloWorld();
            }

        }

    }

	public interface HelloWorldService {
		void helloWorld();
	}
}
