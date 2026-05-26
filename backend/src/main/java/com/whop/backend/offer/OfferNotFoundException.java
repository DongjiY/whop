package com.whop.backend.offer;

public class OfferNotFoundException extends RuntimeException {
    public OfferNotFoundException() {
        super("Offer not found");
    }
}
