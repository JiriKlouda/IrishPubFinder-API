package com.irishpubfinder.api.exception;

public class DuplicateFriendRequestException extends RuntimeException {
    public DuplicateFriendRequestException(String message) {
        super(message);
    }
}
