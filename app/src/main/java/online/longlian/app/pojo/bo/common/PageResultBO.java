package online.longlian.app.pojo.bo.common;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PageResultBO<T>{
        private List<T> list;
        private Long total;
}
