package com.whop.backend.offer;

public class ForbiddenOfferActionException extends RuntimeException {
    public ForbiddenOfferActionException() {
        super("You are not allowed to perform this offer action");
    }
}
