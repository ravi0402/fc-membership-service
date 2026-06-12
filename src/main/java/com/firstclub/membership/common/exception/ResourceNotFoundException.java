package com.firstclub.membership.common.exception;

/** Thrown when a referenced resource (plan, tier, subscription, ...) does not exist. → 404. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException of(String entity, Object id) {
        return new ResourceNotFoundException(entity + " not found: " + id);
    }
}
