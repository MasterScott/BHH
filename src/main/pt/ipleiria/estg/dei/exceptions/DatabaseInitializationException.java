package main.pt.ipleiria.estg.dei.exceptions;

public class DatabaseInitializationException extends RuntimeException {
    public DatabaseInitializationException(String message) {
        super(message);
    }
}