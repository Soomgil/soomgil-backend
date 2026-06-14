package com.soomgil.common.api;

import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;

public abstract class ApiControllerSupport {

	protected final <T> T notImplemented() {
		throw new BusinessException(ErrorCode.NOT_IMPLEMENTED, "Endpoint contract is scaffolded only.");
	}
}
