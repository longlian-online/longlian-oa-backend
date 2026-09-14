package online.longlian.app.pojo.bo.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.InputStream;

@Getter
@Builder
@ToString(exclude = "content")
@NoArgsConstructor
@AllArgsConstructor
public class LocalFileWriteParamsBO {
    private String storageKey;
    private InputStream content;
    private Long expectedSize;
    private String expectedMimeType;
}
