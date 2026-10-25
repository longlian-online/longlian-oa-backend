package online.longlian.app.service.outbox;

public class OutboxPermanentException extends RuntimeException {
    public OutboxPermanentException(String message) {
        super(message);
    }
}
