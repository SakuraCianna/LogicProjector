package com.LogicProjector.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.LogicProjector.analysis.UnsupportedAlgorithmException;
import com.LogicProjector.auth.AuthException;
import com.LogicProjector.exporttask.ExportTaskException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler({AuthException.class, IllegalArgumentException.class, UnsupportedAlgorithmException.class})
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public Map<String, String> handleMessageException(RuntimeException exception) {
        return Map.of("message", exception.getMessage());
    }

    @ExceptionHandler(ExportTaskException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public Map<String, String> handleExportFailure(ExportTaskException exception) {
        return Map.of("message", exception.getMessage());
    }
}
