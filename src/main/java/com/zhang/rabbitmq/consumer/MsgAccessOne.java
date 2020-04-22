package com.zhang.rabbitmq.consumer;

import com.zhang.rabbitmq.comfig.RabbitMqConfig;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * @author zhang
 * @version 1.0
 * @date 2020/4/22 20:33
 * 消息接收测试类
 */
@Component
public class MsgAccessOne {

    //@RabbitListener(queues = RabbitMqConfig.QUEUE_ONE)
    public void msgAccessOne(Message message) {
        if (message != null) {
            byte[] body = message.getBody();
            System.out.println("我接收到的消息是: " + new String(body));
        }
    }
}
