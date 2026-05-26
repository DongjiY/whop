package com.whop.backend.offer;

public class DuplicateOfferException extends RuntimeException {
    public DuplicateOfferException() {
        super("You already submitted an offer for this task");
    }
}
