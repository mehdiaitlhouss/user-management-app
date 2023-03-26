package com.miola.backend.exception.domain;

public class UsernameExistException extends Exception{

    public UsernameExistException(String message) {
        super(message);
    }
}
