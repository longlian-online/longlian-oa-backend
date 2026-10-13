package online.longlian.app.pojo.bo.common;

import java.util.List;

/** Server-owned input definition; keys identify submitted values and labels are user-facing. */
public record TaskFormField(String key, String label, String type, Boolean required, List<String> options) {
}
