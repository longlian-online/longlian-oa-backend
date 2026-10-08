package online.longlian.app.pojo.bo.common;

import java.util.List;

public record TaskFormField(String key, String label, String type, Boolean required, List<String> options) {
}
