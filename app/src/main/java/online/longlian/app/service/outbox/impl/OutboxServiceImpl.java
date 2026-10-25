package online.longlian.app.service.outbox.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import online.longlian.app.mapper.OutboxMessageMapper;
import online.longlian.app.pojo.entity.OutboxMessage;
import online.longlian.app.service.outbox.OutboxService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OutboxServiceImpl extends ServiceImpl<OutboxMessageMapper, OutboxMessage> implements OutboxService {

    @Override
    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public void enqueueAll(List<OutboxMessage> messages) {
        saveBatch(messages);
    }
}
