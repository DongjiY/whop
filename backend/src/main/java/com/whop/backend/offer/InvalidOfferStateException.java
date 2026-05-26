package com.whop.backend.offer;

public class InvalidOfferStateException extends RuntimeException {
    public InvalidOfferStateException() {
        super("Offer cannot be changed from its current state");
    }
}
