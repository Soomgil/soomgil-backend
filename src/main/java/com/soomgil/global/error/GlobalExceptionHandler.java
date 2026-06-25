package com.soomgil.global.error;

import com.soomgil.common.api.dto.ProblemDetails;
import com.soomgil.common.api.dto.ProblemField;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Controller 계층에서 올라온 예외를 공통 ProblemDetails 응답으로 변환한다.
 *
 * <p>입력값 검증 실패는 {@code VALIDATION_FAILED}, JSON 파싱처럼 요청 자체를 읽을 수 없는 경우는
 * {@code INVALID_REQUEST}, 예상하지 못한 예외는 {@code INTERNAL_ERROR}로 매핑한다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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

	@ExceptionHandler(MissingServletRequestParameterException.class)
	ResponseEntity<ProblemDetails> handleMissingServletRequestParameter(
		MissingServletRequestParameterException exception,
		HttpServletRequest request
	) {
		List<ProblemField> fields = List.of(new ProblemField(
			exception.getParameterName(),
			"Required request parameter is missing."
		));
		ProblemDetails problemDetail = problemDetailsFactory.create(
			ErrorCode.VALIDATION_FAILED,
			ErrorCode.VALIDATION_FAILED.defaultMessage(),
			request,
			fields
		);
		return ResponseEntity.badRequest().body(problemDetail);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	ResponseEntity<ProblemDetails> handleMethodArgumentTypeMismatch(
		MethodArgumentTypeMismatchException exception,
		HttpServletRequest request
	) {
		List<ProblemField> fields = List.of(new ProblemField(exception.getName(), "Invalid value."));
		ProblemDetails problemDetail = problemDetailsFactory.create(
			ErrorCode.VALIDATION_FAILED,
			ErrorCode.VALIDATION_FAILED.defaultMessage(),
			request,
			fields
		);
		return ResponseEntity.badRequest().body(problemDetail);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ResponseEntity<ProblemDetails> handleHttpMessageNotReadable(
		HttpMessageNotReadableException exception,
		HttpServletRequest request
	) {
		ProblemDetails problemDetail = problemDetailsFactory.create(
			ErrorCode.INVALID_REQUEST,
			"Malformed request body.",
			request,
			List.of()
		);
		return ResponseEntity.badRequest().body(problemDetail);
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ProblemDetails> handleUnexpected(Exception exception, HttpServletRequest request) {
		log.error("Unexpected server error while handling {} {}", request.getMethod(), request.getRequestURI(), exception);
		ProblemDetails problemDetail = problemDetailsFactory.create(
			ErrorCode.INTERNAL_ERROR,
			ErrorCode.INTERNAL_ERROR.defaultMessage(),
			request,
			List.of()
		);
		return ResponseEntity.internalServerError().body(problemDetail);
	}
}
