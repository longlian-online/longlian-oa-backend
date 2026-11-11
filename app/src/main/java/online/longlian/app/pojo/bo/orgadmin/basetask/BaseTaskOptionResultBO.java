package online.longlian.app.pojo.bo.orgadmin.basetask;

import online.longlian.common.enumeration.Status;

public record BaseTaskOptionResultBO(
        Long id,
        String name,
        String icon,
        String description,
        Status status) {
}
