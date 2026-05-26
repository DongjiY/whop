package com.whop.backend.auth;

import com.whop.backend.auth.AuthDtos.ApiError;
import com.whop.backend.offer.DuplicateOfferException;
import com.whop.backend.offer.ForbiddenOfferActionException;
import com.whop.backend.offer.InvalidOfferStateException;
import com.whop.backend.offer.OfferNotFoundException;
import com.whop.backend.task.TaskNotFoundException;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(DuplicateUsernameException.class)
    public ResponseEntity<ApiError> handleDuplicateUsername(DuplicateUsernameException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError("DUPLICATE_USERNAME", exception.getMessage()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiError("INVALID_CREDENTIALS", "Invalid username or password"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        String message =
                exception.getBindingResult().getFieldErrors().stream()
                        .map(FieldError::getDefaultMessage)
                        .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(new ApiError("VALIDATION_ERROR", message));
    }

    @ExceptionHandler(TaskNotFoundException.class)
    public ResponseEntity<ApiError> handleTaskNotFound(TaskNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError("TASK_NOT_FOUND", "Task not found"));
    }

    @ExceptionHandler(OfferNotFoundException.class)
    public ResponseEntity<ApiError> handleOfferNotFound(OfferNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError("OFFER_NOT_FOUND", "Offer not found"));
    }

    @ExceptionHandler(DuplicateOfferException.class)
    public ResponseEntity<ApiError> handleDuplicateOffer(DuplicateOfferException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError("DUPLICATE_OFFER", exception.getMessage()));
    }

    @ExceptionHandler(ForbiddenOfferActionException.class)
    public ResponseEntity<ApiError> handleForbiddenOfferAction(ForbiddenOfferActionException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiError("FORBIDDEN_ACTION", exception.getMessage()));
    }

    @ExceptionHandler(InvalidOfferStateException.class)
    public ResponseEntity<ApiError> handleInvalidOfferState(InvalidOfferStateException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError("INVALID_OFFER_STATE", exception.getMessage()));
    }
}
