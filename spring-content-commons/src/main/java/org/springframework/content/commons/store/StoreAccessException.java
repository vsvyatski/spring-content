package org.springframework.content.commons.store;

import org.springframework.core.NestedRuntimeException;

import java.io.Serial;

public class StoreAccessException extends NestedRuntimeException {
	@Serial
	private static final long serialVersionUID = 6336877111209647510L;

	public StoreAccessException(String msg) {
		super(msg);
	}

	public StoreAccessException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
