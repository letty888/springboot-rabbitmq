package com.zhang.rabbitmq.comfig;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.CorrelationData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 *
 * @author: zhangHuan
 * @date: 2020/05/28/18:01
 * @Description: 带有confirm和return机制的消息发送类(主要作用于 生产者 - - > 交互机 - - > 队列)
 */
@Component
@Slf4j
public class SendMessageConfirm implements RabbitTemplate.ConfirmCallback, RabbitTemplate.ReturnCallback {

    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private RedisTemplate redisTemplate;

    public static final String MESSAGE_CONFIRM_KEY = "message_confirm_";

    public SendMessageConfirm(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
        rabbitTemplate.setConfirmCallback(this);
        rabbitTemplate.setReturnCallback(this);
    }


    /**
     * 2.交互机接收到消息后的回馈(消息从生产者-->交互机(确认模式))
     *
     * @param correlationData 相关配置信息
     * @param ack             交互机是否成功收到了消息
     * @param cause           交互机没有收到消息的原因
     */
    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {

        //从redis中查询消息的相关信息
        Map<String, String> messageMap
                = redisTemplate.boundHashOps(MESSAGE_CONFIRM_KEY + correlationData.getId()).entries();
        String exchange = messageMap.get("exchange");
        String routingKey = messageMap.get("routingKey");
        String jsonMessage = messageMap.get("message");

        if (ack) {
            log.info("{}交互机成功收到了消息,删除掉redis中保存的消息相关信息", exchange);
            redisTemplate.delete(correlationData.getId());
            redisTemplate.delete(MESSAGE_CONFIRM_KEY + correlationData.getId());
        } else {
            //交互机没有成功收到消息,则从redis中查询消息的相关信息并且重新发送
            log.info("{}交互机没有成功收到消息", exchange);
            log.info("没有成功收到消息的原因是:" + cause);
            log.info(",重新发送消息");
            rabbitTemplate.convertAndSend(exchange, routingKey, jsonMessage);
        }
    }


    /**
     * 3:  队列没有接收到消息时的回馈(消息从交互机发送到队列-->回退模式)
     * 1.在配置文件中开启回退模式
     * publisher-returns: true
     * 2.消息从交互机发送到队列失败时才会执行 returnCallback
     * 3.设置交互机的处理模式
     * a.如果消息没有成功路由到队列,则消息丢弃(默认方式,不需要设置)
     * <p>
     * b.如果消息没有成功路由到队列,则消息返回给交互机(需要在代码中设置)
     * rabbitTemplate.setMandatory(true);
     *
     * @param message 消息对象
     * @param i       错误码
     * @param s       错误信息
     * @param s1      交互机
     * @param s2      路由key
     */
    @Override
    public void returnedMessage(Message message, int i, String s, String s1, String s2) {

        //设置消息路由失败后的处理模式-->为true的话消息返回给交互机
        rabbitTemplate.setMandatory(true);
        log.error("{}类中,消息由交换机-->指定队列路由失败...", "SendMessageConfirm");
        log.error("错误码:{},原因:{}", i, s);
        log.error("丢失的消息是:" + message.toString());

    }


    /**
     * 1.发送消息(发送消息之前先将消息的相关信息保存到redis中,目的就是防止消息丢失)
     *
     * @param exchange   交互机
     * @param routingKey 路由key
     * @param message    要发送的消息
     */
    public void sendMessage(String exchange, String routingKey, Object message) {

        log.info("{}类中将消息转为json字符串", "SendMessageConfirm");
        String jsonMessage = JSON.toJSONString(message);
        CorrelationData correlationData = new CorrelationData(UUID.randomUUID().toString());
        log.info("{}类中,以消息的唯一标识作为key,消息的json字符串作为值以字符串类型保存到redis中", "SendMessageConfirm");
        redisTemplate.boundValueOps(correlationData.getId()).set(jsonMessage);

        Map<String, String> map = new HashMap<>(0);
        map.put("exchange", exchange);
        map.put("routingKey", routingKey);
        map.put("message", jsonMessage);
        log.info("{}类中,以指定标识作为key,消息的相关信息作为值以hash类型保存到redis中", "SendMessageConfirm");
        redisTemplate.boundHashOps(MESSAGE_CONFIRM_KEY + correlationData.getId()).putAll(map);


        //携带着本次消息的唯一标识,进行数据发送
        rabbitTemplate.convertAndSend(exchange, routingKey, jsonMessage, correlationData);
    }
}
