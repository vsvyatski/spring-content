package org.springframework.content.commons.utils;

import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;

public class ReflectionServiceImpl implements ReflectionService {

	public ReflectionServiceImpl() {
	}

	@Override
	public Object invokeMethod(Method method, Object target, Object... args) {
		return ReflectionUtils.invokeMethod(method, target, args);
	}
}
