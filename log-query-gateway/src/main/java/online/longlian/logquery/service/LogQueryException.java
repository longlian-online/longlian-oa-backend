package online.longlian.logquery.service;

public class LogQueryException extends RuntimeException {

    private final int status;

    public LogQueryException(int status, String message) {
        super(message);
        this.status = status;
    }

    public LogQueryException(int status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
