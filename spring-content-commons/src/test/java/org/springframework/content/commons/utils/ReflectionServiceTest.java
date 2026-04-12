package org.springframework.content.commons.utils;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import org.junit.runner.RunWith;
import org.springframework.util.ReflectionUtils;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(Ginkgo4jRunner.class)
public class ReflectionServiceTest {

	private ReflectionService reflectionService;

	// mocks
	private HelloWorldService service;

	{
		Describe("ReflectionService", () -> Context("invokeMethod", () -> {
            BeforeEach(() -> service = mock(HelloWorldService.class));
            JustBeforeEach(() -> {
                reflectionService = new ReflectionServiceImpl();
                reflectionService.invokeMethod(ReflectionUtils
                        .findMethod(HelloWorldService.class, "helloWorld"), service
                );
            });
            It("should invoke the method", () -> verify(service).helloWorld());
        }));
	}

	public interface HelloWorldService {
		void helloWorld();
	}
}
