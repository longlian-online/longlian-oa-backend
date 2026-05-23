package online.longlian.app.pojo.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.longlian.app.common.enumeration.InboxLinkType;
import online.longlian.app.common.enumeration.InboxMessageType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InboxMessageBO {

    private InboxMessageType type;

    private String title;

    private String content;

    private InboxLinkType linkType;

    private String linkValue;

    private String relatedType;

    private Long relatedId;
}
