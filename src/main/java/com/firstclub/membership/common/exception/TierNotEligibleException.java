package com.firstclub.membership.common.exception;

/** Thrown when a user does not meet the criteria required to move to a gated tier. → 409. */
public class TierNotEligibleException extends RuntimeException {

    public TierNotEligibleException(String userId, String tierCode) {
        super("User " + userId + " is not eligible for tier " + tierCode);
    }
}
