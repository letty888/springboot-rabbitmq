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
public class RabbitMqConfigCeshi {

    /**
     * 普通交互机one(重定向模式)
     */
    public static final String GENERAL_EXCHANGE_ONE = "general_exchange_one";

    /**
     * 普通队列one
     */
    public static final String GENERAL_QUEUE_ONE = "general_queue_one";

    public static final String ROUTING_KEY_ONE = "one";


    /**
     * 死信交互机和队列
     */
    public static final String DEAD_LETTER_EXCHANGE = "DEAD_LETTER_EXCHANGE";
    public static final String DEAD_LETTER_QUEUE = "DEAD_LETTER_QUEUE";
    public static final String DEAD_LETTER_REDIRECT_ROUTING_KEY = "dead.#";


    /**
     * 申明队列 GENERAL_QUEUE_ONE (开启了队列的持久化,并且这个队列绑定了一个死信交互机)
     *
     * @return Queue
     */
    @Bean(GENERAL_QUEUE_ONE)
    public Queue GENERAL_QUEUE_ONE() {
        Map<String, Object> args = new HashMap<>(2);
//       x-dead-letter-exchange    声明  死信队列Exchange
        args.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
        //args.put("x-message-ttl", 5000);  //使用延时功能时才设置过期时间
//       x-dead-letter-routing-key    声明 死信队列抛出异常重定向队列的routingKey(TKEY_R)
        args.put("x-dead-letter-routing-key", DEAD_LETTER_REDIRECT_ROUTING_KEY);
        return QueueBuilder.durable(GENERAL_QUEUE_ONE).withArguments(args).build();
    }

    /**
     * 申明(重定向模式)的交互机 : GENERAL_EXCHANGE_ONE (开启了交互机的持久化)
     *
     * @return Exchange
     */
    @Bean(GENERAL_EXCHANGE_ONE)
    public Exchange GENERAL_EXCHANGE_ONE() {
        return ExchangeBuilder.directExchange(GENERAL_EXCHANGE_ONE).durable(true).build();
    }

    /**
     * 绑定 交互机 GENERAL_EXCHANGE_ONE 和 队列 GENERAL_QUEUE_ONE ,ROUTING_KEY_ONE 为 "one"
     *
     * @param exchange GENERAL_EXCHANGE_ONE
     * @param queue    GENERAL_QUEUE_ONE
     * @return Binding
     */
    @Bean
    public Binding GENERAL_EXCHANGE_ONE_AND_GENERAL_QUEUE_ONE(@Qualifier(GENERAL_EXCHANGE_ONE) Exchange exchange, @Qualifier(GENERAL_QUEUE_ONE) Queue queue) {
        return BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY_ONE).noargs();
    }

    /**
     * 申明死信交换机
     * 注意:死信队列跟交换机类型没有关系 不一定为directExchange  不影响该类型交换机的特性.
     *
     * @return Exchange 死信交换机
     */
    @Bean(DEAD_LETTER_EXCHANGE)
    public Exchange deadLetterExchange() {
        return ExchangeBuilder.topicExchange(DEAD_LETTER_EXCHANGE).durable(true).build();
    }

    /**
     * 申明死信队列
     *
     * @return Queue 死信队列
     */
    @Bean(DEAD_LETTER_QUEUE)
    public Queue deadLetterQueue() {
        return new Queue(DEAD_LETTER_QUEUE, true);
    }

    /**
     * @param exchange 死信交换机
     * @param queue    死信队列
     * @return 绑定 死信交换机 和  死信队列
     */
    @Bean
    public Binding deadLetterBinding(@Qualifier(DEAD_LETTER_EXCHANGE) Exchange exchange, @Qualifier(DEAD_LETTER_QUEUE) Queue queue) {
        return BindingBuilder.bind(queue).to(exchange).with(DEAD_LETTER_REDIRECT_ROUTING_KEY).noargs();
    }


    /**
     * @param exchange 死信交换机
     * @param queue    普通队列
     * @return 绑定 死信交换机 和  普通队列
     */
    @Bean
    public Binding deadExchangeAndGneralQueueBinding(@Qualifier(DEAD_LETTER_EXCHANGE) Exchange exchange, @Qualifier(GENERAL_QUEUE_ONE) Queue queue) {
        return BindingBuilder.bind(queue).to(exchange).with("").noargs();
    }
}
