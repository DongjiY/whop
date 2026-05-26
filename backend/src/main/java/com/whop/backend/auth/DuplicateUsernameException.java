package com.whop.backend.auth;

public class DuplicateUsernameException extends RuntimeException {
    public DuplicateUsernameException() {
        super("Username is already taken");
    }
}
