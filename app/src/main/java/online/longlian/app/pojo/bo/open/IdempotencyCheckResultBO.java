package online.longlian.app.pojo.bo.open;

/**
 * 幂等查重结果。
 * MISS=首笔，可执行业务；HIT=命中同方法/路径/请求体的首次结果，直接回放 data；
 * CONFLICT=同键不同请求体，或首笔并发提交尚未落库。
 */
public record IdempotencyCheckResultBO(Outcome outcome, Object data) {

    public enum Outcome {
        MISS, HIT, CONFLICT
    }

    public static IdempotencyCheckResultBO miss() {
        return new IdempotencyCheckResultBO(Outcome.MISS, null);
    }

    public static IdempotencyCheckResultBO hit(Object data) {
        return new IdempotencyCheckResultBO(Outcome.HIT, data);
    }

    public static IdempotencyCheckResultBO conflict() {
        return new IdempotencyCheckResultBO(Outcome.CONFLICT, null);
    }
}
