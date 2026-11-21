package online.longlian.app.pojo.bo.open;

/**
 * 幂等查重结果。
 * MISS=首笔，可执行业务；HIT=命中同方法/路径/请求体的首次结果，直接回放 data；
 * CONFLICT=同键不同请求体，或首笔并发提交尚未落库。
 * 独立顶层枚举：record 内嵌套类型会触发 DeepSource W1035 对 record 实例化的误报。
 */
public enum IdempotencyCheckOutcome {
    MISS, HIT, CONFLICT
}
