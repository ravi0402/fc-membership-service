package com.firstclub.membership.common.exception;

/** Thrown when a request is well-formed but violates a domain rule. → 422. */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
