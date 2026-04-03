package com.yupi.yuaicodemother.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.yupi.yuaicodemother.model.entity.ChatHistory;
import com.yupi.yuaicodemother.mapper.ChatHistoryMapper;
import com.yupi.yuaicodemother.service.ChatHistoryService;
import org.springframework.stereotype.Service;

/**
 *  服务层实现。
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 */
@Service
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory>  implements ChatHistoryService{

}
