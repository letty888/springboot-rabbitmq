package com.zhang.rabbitmq.listener;

import com.rabbitmq.client.Channel;
import com.zhang.rabbitmq.comfig.RabbitMqConfig;
import com.zhang.rabbitmq.comfig.RabbitMqConfigCeshi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.ChannelAwareMessageListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;

/**
 * 标准的消息监听类
 *
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
@Slf4j
public class AckListener implements ChannelAwareMessageListener {

    @Override
    @RabbitListener(queues = RabbitMqConfigCeshi.GENERAL_QUEUE_ONE)
    public void onMessage(Message message, Channel channel) throws Exception {

        if (message == null) {
            log.error("{}监听器中收到的消息为空,消息可能丢失", "AckListener");
            throw new RuntimeException("AckListener监听器中收到的消息为空,消息可能丢失");
        }
        log.info("{}监听器中接收到的消息是:{}", "AckListener", message.toString());

        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            //消费者的预抓取总数设置(即削峰填谷,官方建议消费者的预抓取总数为100-300)
            channel.basicQos(300);

            //调用业务层处理业务逻辑
            //todo

            //int i = 3 / 0;
            //注释这里的手动确认是为了测试mq的削峰填谷功能,即上面的  channel.basicQos(300);
            channel.basicAck(deliveryTag, false);
            log.info("deliveryTag==>" + deliveryTag);
            //注意,这里抓最大的异常Exception
        } catch (Exception e) {
            e.printStackTrace();
            log.error("{}监听器中接收到消息后处理业务逻辑时出现异常", "AckListener");
            log.info("消息重新回到{}队列中", RabbitMqConfigCeshi.GENERAL_QUEUE_ONE);
            //4.若处理消息的过程中出现异常,则调用channel的basicNack()方法进行拒绝签收,让消息重回队列中并重新发送过来
            /**
             * 参数一:delivery_tag是消息投递序号，每个channel对应一个(long类型)，从1开始到9223372036854775807范围，在手动消息确认时可以对指定delivery_tag的消息进行ack、nack、reject等操作
             * 参数二:true所有消费者都会拒绝这个消息,false只有当前消费者拒绝
             * 参数三:设置为true的话表示消息处理异常时将重回队列中
             */
            channel.basicNack(deliveryTag, false, true);
        }
    }
}
