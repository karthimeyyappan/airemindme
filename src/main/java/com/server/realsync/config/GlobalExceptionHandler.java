/**
 * 
 */
package com.server.realsync.config;

/**
 * 
 */
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<String> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        // Extract the specific cause of the exception
        String message = ex.getMostSpecificCause().getMessage();
        
        // Check if it is a duplicate entry error
        if (message.contains("Duplicate entry")) {
            return new ResponseEntity<>("Duplicate entry: " + message, HttpStatus.CONFLICT);
        }
        
        // For other DataIntegrityViolationExceptions
        return new ResponseEntity<>("Data integrity violation: " + message, HttpStatus.BAD_REQUEST);
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public Object handleAccountNotFoundException(AccountNotFoundException ex, jakarta.servlet.http.HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri.startsWith("/api/")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        }
        return "redirect:/no-account";
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
    }

    /*@ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<String> handleResourceNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }*/
}