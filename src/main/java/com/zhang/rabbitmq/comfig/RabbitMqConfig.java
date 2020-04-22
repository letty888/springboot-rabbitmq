package com.zhang.rabbitmq.comfig;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * @author zhang
 * @version 1.0
 * @date 2020/4/22 20:06
 */
@Configuration
public class RabbitMqConfig {

    /**
     * 普通交互机和队列
     */
    public static final String EXCHANGE_TOPIC_ONE = "EXCHANGE_TOPIC_ONE";
    public static final String QUEUE_ONE = "QUEUE_ONE";
    public static final String ROUTING_KEY = "#.demo";

    /**
     * 定义交互机
     *
     * @return Exchange 通配符型的交互机1
     */
    @Bean("exchange_topic_one")
    public Exchange exchangeOne() {

        return ExchangeBuilder.topicExchange(EXCHANGE_TOPIC_ONE).durable(true).build();
    }

    /**
     * 定义队列
     *
     * @return Queue 队列1
     */
    @Bean("queue_one")
    public Queue queueOne() {
     /*   Map<String, Object> args = new HashMap<>(2);
//       x-dead-letter-exchange    声明  死信队列Exchange
        args.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//       x-dead-letter-routing-key    声明 死信队列抛出异常重定向队列的routingKey(TKEY_R)
        args.put("x-dead-letter-routing-key", DEAD_LETTER_REDIRECT_ROUTING_KEY);
        //设置过期时间的目的:   ttl+死信队列 就可以达到消息延迟的功能
        //args.put("x-expires", 10000);*/
        //return QueueBuilder.durable(QUEUE_ONE).withArguments(args).build();
        return QueueBuilder.durable(QUEUE_ONE).build();
    }

    /**
     * 绑定交互机和队列
     *
     * @param queue    队列1
     * @param exchange 通配符型的交互机1
     * @return 绑定关系
     */
    @Bean
    public Binding generalBinding(@Qualifier("queue_one") Queue queue, @Qualifier("exchange_topic_one") Exchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY).noargs();
    }


    /**
     * 死信交互机和队列
     */
    public static final String DEAD_LETTER_EXCHANGE = "DEAD_LETTER_EXCHANGE";
    public static final String DEAD_LETTER_QUEUE = "DEAD_LETTER_QUEUE";
    public static final String DEAD_LETTER_REDIRECT_ROUTING_KEY = "dead.#";

    /**
     * 申明死信交换机
     * 注意:死信队列跟交换机类型没有关系 不一定为directExchange  不影响该类型交换机的特性.
     *
     * @return Exchange 死信交换机
     */
  /*  @Bean("deadLetterExchange")
    public Exchange deadLetterExchange() {
        return ExchangeBuilder.topicExchange(DEAD_LETTER_EXCHANGE).durable(true).build();
    }

    *//**
     * 申明死信队列
     *
     * @return Queue 死信队列
     *//*
    @Bean("deadLetterQueue")
    public Queue deadLetterQueue() {
        Map<String, Object> args = new HashMap<>(2);
//       x-dead-letter-exchange    声明  死信队列Exchange
        args.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//       x-dead-letter-routing-key    声明 死信队列抛出异常重定向队列的routingKey(TKEY_R)
        args.put("x-dead-letter-routing-key", DEAD_LETTER_REDIRECT_ROUTING_KEY);
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).withArguments(args).build();
    }

    *//**
     * @param exchange 死信交换机
     * @param queue    死信队列
     * @return 绑定 死信交换机 和  死信队列
     *//*
    @Bean
    public Binding deadLetterBinding(@Qualifier("deadLetterExchange") Exchange exchange, @Qualifier("deadLetterQueue") Queue queue) {
        return BindingBuilder.bind(queue).to(exchange).with(DEAD_LETTER_REDIRECT_ROUTING_KEY).noargs();
    }


    *//**
     * @param exchange 死信交换机
     * @param queue    普通队列
     * @return 绑定 死信交换机 和  普通队列
     *//*
    @Bean
    public Binding deadExchangeAndGneralQueueBinding(@Qualifier("deadLetterExchange") Exchange exchange, @Qualifier("queue_one") Queue queue) {
        return BindingBuilder.bind(queue).to(exchange).with(DEAD_LETTER_REDIRECT_ROUTING_KEY).noargs();
    }*/

}
