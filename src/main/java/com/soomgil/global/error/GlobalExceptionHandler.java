package com.soomgil.global.error;

import com.soomgil.common.api.dto.ProblemDetails;
import com.soomgil.common.api.dto.ProblemField;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private final ProblemDetailsFactory problemDetailsFactory;

	public GlobalExceptionHandler(ProblemDetailsFactory problemDetailsFactory) {
		this.problemDetailsFactory = problemDetailsFactory;
	}

	@ExceptionHandler(BusinessException.class)
	ResponseEntity<ProblemDetails> handleBusinessException(BusinessException exception, HttpServletRequest request) {
		ErrorCode errorCode = exception.errorCode();
		ProblemDetails problemDetail = problemDetailsFactory.create(errorCode, exception.getMessage(), request, List.of());
		return ResponseEntity.status(errorCode.status()).body(problemDetail);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ProblemDetails> handleMethodArgumentNotValid(
		MethodArgumentNotValidException exception,
		HttpServletRequest request
	) {
		List<ProblemField> fields = exception.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(error -> new ProblemField(error.getField(), error.getDefaultMessage()))
			.toList();
		ProblemDetails problemDetail = problemDetailsFactory.create(
			ErrorCode.VALIDATION_FAILED,
			ErrorCode.VALIDATION_FAILED.defaultMessage(),
			request,
			fields
		);
		return ResponseEntity.badRequest().body(problemDetail);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	ResponseEntity<ProblemDetails> handleConstraintViolation(
		ConstraintViolationException exception,
		HttpServletRequest request
	) {
		List<ProblemField> fields = exception.getConstraintViolations()
			.stream()
			.map(violation -> new ProblemField(violation.getPropertyPath().toString(), violation.getMessage()))
			.toList();
		ProblemDetails problemDetail = problemDetailsFactory.create(
			ErrorCode.VALIDATION_FAILED,
			ErrorCode.VALIDATION_FAILED.defaultMessage(),
			request,
			fields
		);
		return ResponseEntity.badRequest().body(problemDetail);
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ProblemDetails> handleUnexpected(Exception exception, HttpServletRequest request) {
		ProblemDetails problemDetail = problemDetailsFactory.create(
			ErrorCode.INTERNAL_ERROR,
			ErrorCode.INTERNAL_ERROR.defaultMessage(),
			request,
			List.of()
		);
		return ResponseEntity.internalServerError().body(problemDetail);
	}
}
