package com.example.inventory.exception;

public class ResourceInUseException extends RuntimeException {
    public ResourceInUseException(String message) { super(message); }
}
