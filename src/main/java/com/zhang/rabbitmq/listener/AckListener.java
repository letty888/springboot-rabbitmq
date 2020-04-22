package com.zhang.rabbitmq.listener;

import com.rabbitmq.client.Channel;
import com.zhang.rabbitmq.comfig.RabbitMqConfig;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.ChannelAwareMessageListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;

/**
 * @author zhang
 * @version 1.0
 * @date 2020/4/22 21:55
 * 消费者 ack 机制步骤:
 * 1.配置文件中设置手动处理模式:
 * acknowledge-mode: manual
 * 2.让自定义的监听器实现ChannelAwareMessageListener接口;
 * 3.用注解显示申明监听哪个/些队列
 * 4.如果消息成功处理,则调用channel的basicAck()方法进行签收;
 * 5.如果消息处理失败,则调用channel的basicNack()方法进行拒绝签收;
 */
@Component
public class AckListener implements ChannelAwareMessageListener {

    @Override
    @RabbitListener(queues = RabbitMqConfig.QUEUE_ONE)
    public void onMessage(Message message, Channel channel) throws Exception {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            //1.接收转化消息
            System.out.println(new String(message.getBody()));

            //2.利用消息进行业务处理
            System.out.println("处理业务逻辑");
            //int i = 3/0;
            //3.手动签收
            channel.basicAck(deliveryTag, true);

            //注意,这里抓最大的异常Exception
        } catch (Exception e) {
            //4.若处理消息的过程中出现异常,则调用channel的basicNack()方法进行拒绝签收,让消息重回队列中并重新发送过来
            /**
             * 参数一:delivery_tag是消息投递序号，每个channel对应一个(long类型)，从1开始到9223372036854775807范围，在手动消息确认时可以对指定delivery_tag的消息进行ack、nack、reject等操作
             * 参数二:设置为true的话表示可以一次接受多个消息
             * 参数三:设置为true的话表示消息处理异常时将重回队列中
             */
            channel.basicNack(deliveryTag, true, true);
        }
    }
}
