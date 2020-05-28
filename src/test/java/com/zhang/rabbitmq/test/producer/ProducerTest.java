package com.zhang.rabbitmq.test.producer;

import com.zhang.rabbitmq.comfig.RabbitMqConfig;
import com.zhang.rabbitmq.comfig.RabbitMqConfigCeshi;
import com.zhang.rabbitmq.comfig.SendMessageConfirm;
import com.zhang.rabbitmq.pojo.User;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.CorrelationData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author zhang
 * @version 1.0
 * @date 2020/4/22 20:25
 */
@SpringBootTest
@RunWith(SpringRunner.class)
public class ProducerTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private SendMessageConfirm sendMessageConfirm;

    /**
     * 测试:  消息从生产者发送到交互机-->确认模式
     */
    @Test
    public void testSendMsgConfirm() {
        User user = new User(4, 86, "张三丰");
        sendMessageConfirm.sendMessage(RabbitMqConfigCeshi.GENERAL_EXCHANGE_ONE, RabbitMqConfigCeshi.ROUTING_KEY_ONE, user);
    }

    /**
     * 测试: 消息从交互机路由到队列-->回退模式
     */
    @Test
    public void testSendMsgReturn() {
            User user = new User(666888999, 86, "张三丰");
            sendMessageConfirm.sendMessage(RabbitMqConfigCeshi.GENERAL_EXCHANGE_ONE, RabbitMqConfigCeshi.ROUTING_KEY_ONE , user);
    }


    /**
     * 测试:  消息从生产者发送到交互机-->确认模式
     */
    @Test
    public void testSendMsgConfirm1() {
        //2.接收回调函数并针对消息是否成功发送到交互机的结果做以处理
        //注意:这个方法必须写到发送消息的步骤之前,不然的话debug发现不会走这一步
        rabbitTemplate.setConfirmCallback(new RabbitTemplate.ConfirmCallback() {
            /**
             *
             * @param correlationData 配置相关的参数
             * @param b 消息是否成功从生产者发送到指定的交换机
             * @param s 消息发送失败的原因
             */
            @Override
            public void confirm(CorrelationData correlationData, boolean b, String s) {
                System.out.println("判断消息是否成功发送到交换机的方法执行了...");
                if (b) {
                    System.out.println("消息成功发送到了交换机...");
                } else {
                    System.out.println("消息没有成功发送到交换机,失败的原因是:" + s);
                }
            }
        });
        //1.发送消息
        rabbitTemplate.convertAndSend(RabbitMqConfig.EXCHANGE_TOPIC_ONE, "test02.demo", "我是中国人,啦啦啦啦...");
    }

    /**
     * 测试:  消息从交互机发送到队列-->回退模式
     * 1.在配置文件中开启回退模式
     * publisher-returns: true
     * 2.消息从交互机发送到队列失败时才会执行 returnCallback
     * 3.设置交互机的处理模式
     * a.如果消息没有成功路由到队列,则消息丢弃(默认方式,不需要设置)
     * <p>
     * b.如果消息没有成功路由到队列,则消息返回给交互机(需要在代码中设置)
     * rabbitTemplate.setMandatory(true);
     */
    @Test
    public void testSendMsgReturn1() {
        //2.接收回调函数并针对消息是否成功从交互机路由到队列的结果做以处理
        //2.1设置消息路由失败后的处理模式-->为true的话消息返回给交互机
        rabbitTemplate.setMandatory(true);
        //注意:这个方法必须写到发送消息的步骤之前,不然的话debug发现不会走这一步
        rabbitTemplate.setReturnCallback(new RabbitTemplate.ReturnCallback() {
            /**
             *
             * @param message 消息对象
             * @param i 失败的错误码
             * @param s 错误信息
             * @param s1 交互机
             * @param s2 路由key
             */
            @Override
            public void returnedMessage(Message message, int i, String s, String s1, String s2) {
                System.out.println("消息由交换机-->队列路由失败...");
                System.out.println("失败的原因是:" + s);
                System.out.println("消息是:" + message.toString());
            }
        });
        //1.发送消息
        rabbitTemplate.convertAndSend(RabbitMqConfigCeshi.GENERAL_EXCHANGE_ONE, "test02.demo", "我是中国人,啦啦啦啦...");
    }
}
