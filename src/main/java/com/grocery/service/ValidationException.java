package com.grocery.service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Bad input from the client. Carries per-field messages for the form to show. */
public class ValidationException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    public ValidationException(String message) {
        this(message, Map.of());
    }

    public ValidationException(String message, Map<String, String> fieldErrors) {
        super(message);
        this.fieldErrors = Collections.unmodifiableMap(new LinkedHashMap<>(fieldErrors));
    }

    public Map<String, String> fieldErrors() { return fieldErrors; }
}
