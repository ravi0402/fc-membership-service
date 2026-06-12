package com.firstclub.membership.common.exception;

/** Thrown when a user who already has an ACTIVE subscription tries to subscribe again. → 409. */
public class DuplicateActiveSubscriptionException extends RuntimeException {

    public DuplicateActiveSubscriptionException(String userId) {
        super("User already has an active subscription: " + userId);
    }
}
