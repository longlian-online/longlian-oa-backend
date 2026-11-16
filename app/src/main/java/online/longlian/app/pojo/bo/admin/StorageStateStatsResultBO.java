package online.longlian.app.pojo.bo.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StorageStateStatsResultBO {
    private long count;

    private long bytes;
}
