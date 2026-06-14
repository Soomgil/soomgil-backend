package com.soomgil.global.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.util.List;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	ResponseEntity<ProblemDetail> handleBusinessException(BusinessException exception, HttpServletRequest request) {
		ErrorCode errorCode = exception.errorCode();
		ProblemDetail problemDetail = createProblemDetail(errorCode, exception.getMessage(), request);
		return ResponseEntity.status(errorCode.status()).body(problemDetail);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ProblemDetail> handleMethodArgumentNotValid(
		MethodArgumentNotValidException exception,
		HttpServletRequest request
	) {
		ProblemDetail problemDetail = createProblemDetail(
			ErrorCode.VALIDATION_FAILED,
			ErrorCode.VALIDATION_FAILED.defaultMessage(),
			request
		);
		List<FieldViolation> fields = exception.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(error -> new FieldViolation(error.getField(), error.getDefaultMessage()))
			.toList();
		problemDetail.setProperty("fields", fields);
		return ResponseEntity.badRequest().body(problemDetail);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	ResponseEntity<ProblemDetail> handleConstraintViolation(
		ConstraintViolationException exception,
		HttpServletRequest request
	) {
		ProblemDetail problemDetail = createProblemDetail(
			ErrorCode.VALIDATION_FAILED,
			ErrorCode.VALIDATION_FAILED.defaultMessage(),
			request
		);
		List<FieldViolation> fields = exception.getConstraintViolations()
			.stream()
			.map(violation -> new FieldViolation(violation.getPropertyPath().toString(), violation.getMessage()))
			.toList();
		problemDetail.setProperty("fields", fields);
		return ResponseEntity.badRequest().body(problemDetail);
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ProblemDetail> handleUnexpected(Exception exception, HttpServletRequest request) {
		ProblemDetail problemDetail = createProblemDetail(
			ErrorCode.INTERNAL_ERROR,
			ErrorCode.INTERNAL_ERROR.defaultMessage(),
			request
		);
		return ResponseEntity.internalServerError().body(problemDetail);
	}

	private ProblemDetail createProblemDetail(ErrorCode errorCode, String detail, HttpServletRequest request) {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(errorCode.status(), detail);
		problemDetail.setTitle(errorCode.defaultMessage());
		problemDetail.setType(URI.create("https://api.soomgil.example.com/problems/" + errorCode.code().toLowerCase()));
		problemDetail.setInstance(URI.create(request.getRequestURI()));
		problemDetail.setProperty("code", errorCode.code());
		return problemDetail;
	}

	private record FieldViolation(String name, String reason) {
	}
}
