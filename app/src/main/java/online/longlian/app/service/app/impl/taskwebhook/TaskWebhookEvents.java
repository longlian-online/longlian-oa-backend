package online.longlian.app.service.app.impl.taskwebhook;

public final class TaskWebhookEvents {
    public static final String SUBMITTED = "task.submitted";
    public static final String INVALIDATED = "task.submission_invalidated";
    public static final String REJECTED = "rejected";
    public static final String RESET = "reset";

    private TaskWebhookEvents() {
    }
}
