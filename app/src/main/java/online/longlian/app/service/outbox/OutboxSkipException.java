package online.longlian.app.service.outbox;

public class OutboxSkipException extends RuntimeException {
    public OutboxSkipException(String message) {
        super(message);
    }
}
