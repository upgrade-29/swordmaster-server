package com.swordmaster.common;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.util.DisconnectedClientHelper;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    // 게임 로직 에러
    @ExceptionHandler(GameException.class)
    public ResponseEntity<ErrorResponse> handleGame(GameException e) {
        log.warn("GameException: {}", e.getMessage());

        return getError(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    // Valid 검증 실패
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("검증 실패");

        log.warn("MethodArgumentNotValidException: {}", message);

        return getError(HttpStatus.BAD_REQUEST, message);
    }

    // JSON 형식 오류
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("HttpMessageNotReadableException: {}", e.getMessage());

        return getError(HttpStatus.BAD_REQUEST, "JSON 형식 오류, 본문을 읽을 수 없음");
    }

    // 파라미터 타입 불일치
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("MethodArgumentTypeMismatchException: {} = {}", e.getName(), e.getValue());

        return getError(HttpStatus.BAD_REQUEST, "파라미터 타입 불일치");
    }

    // 존재하지 않는 URL
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource() {
        return getError(HttpStatus.NOT_FOUND, "요청을 찾을 수 없음");
    }

    // 예상치 못한 오류
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e, HttpServletRequest request) {
        if (DisconnectedClientHelper.isClientDisconnectedException(e)) {
            log.debug("ClientDisconnectedException: {} {}", request.getMethod(), request.getRequestURI());

            return null;    // 받을 클라이언트가 없음
        }

        log.error("Exception: {} {}", request.getMethod(), request.getRequestURI(), e);

        return getError(HttpStatus.INTERNAL_SERVER_ERROR, "예상치 못한 오류");
    }

    private ResponseEntity<ErrorResponse> getError(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(message));
    }
}
