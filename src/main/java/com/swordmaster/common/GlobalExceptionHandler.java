package com.swordmaster.common;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.util.DisconnectedClientHelper;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    // 게임 로직 에러
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e) {
        log.warn("BusinessException: {}", e.getMessage());

        return ResponseEntity.status(e.getStatus()).body(new ErrorResponse(e.getMessage(), e.getCode()));
    }

    // Valid 검증 실패
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("검증이 실패했습니다.");

        log.warn("MethodArgumentNotValidException: {}", message);

        return getError(HttpStatus.BAD_REQUEST, message);
    }

    // JSON 형식 오류
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("HttpMessageNotReadableException: {}", e.getMessage());

        return getError(HttpStatus.BAD_REQUEST, "JSON 형식 오류, 본문을 읽을 수 없습니다.");
    }

    // 요청 파라미터 누락
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException e) {
        log.warn("MissingServletRequestParameterException: {}", e.getParameterName());

        return getError(HttpStatus.BAD_REQUEST, e.getParameterName() + " 파라미터가 필요합니다.");
    }

    // 요청 파라미터 타입 불일치
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("MethodArgumentTypeMismatchException: {} = {}", e.getName(), e.getValue());

        return getError(HttpStatus.BAD_REQUEST, "파라미터 타입이 일치하지 않습니다.");
    }

    // 낙관적 락 충돌 (특정 데이터 동시 처리)
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(ObjectOptimisticLockingFailureException e) {
        log.warn("ObjectOptimisticLockingFailureException: {}", e.getMessage());

        return getError(HttpStatus.CONFLICT, " 현재 다른 요청이 처리중입니다. 다시 시도해 주세요.");
    }

    // DB 제약 위반 (동시 요청에 의한 Unique 제약 충돌)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException e) {
        log.warn("DataIntegrityViolationException: {}", e.getMostSpecificCause().getMessage());

        return getError(HttpStatus.CONFLICT, "중복된 요청입니다. 다시 시도해 주세요.");
    }

    // 지원하지 않는 HTTP 메서드 (잘못된 HTTP 요청)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("HttpRequestMethodNotSupportedException: {}", e.getMethod());

        return getError(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 요청 방식입니다.");
    }

    // 잘못된 Content-Type (ex_ 본문을 보내면서 'Content-type: application/json' 헤더를 빠뜨리는 경우)
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException e) {
        log.warn("HttpMediaTypeNotSupportedException: {}", e.getContentType());

        return getError(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Content-Type은 application/json 이어야 합니다.");
    }

    // 존재하지 않는 URL
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource() {
        return getError(HttpStatus.NOT_FOUND, "요청을 찾을 수 없습니다.");
    }

    // 예상치 못한 오류
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e, HttpServletRequest request) {
        if (DisconnectedClientHelper.isClientDisconnectedException(e)) {
            log.debug("ClientDisconnectedException: {} {}", request.getMethod(), request.getRequestURI());

            return null;    // 받을 클라이언트가 없음
        }

        log.error("Exception: {} {}", request.getMethod(), request.getRequestURI(), e);

        return getError(HttpStatus.INTERNAL_SERVER_ERROR, "예상치 못한 오류가 발생했습니다.");
    }

    private ResponseEntity<ErrorResponse> getError(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(message));
    }
}
