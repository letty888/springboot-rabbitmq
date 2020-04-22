## rabbitmq高级特性

#### 背景:

​	a.此项目基于springboot整合了rabbitmq;

​		

![image-20200422212011732](C:\Users\Administrator\AppData\Roaming\Typora\typora-user-images\image-20200422212011732.png)

### 一.消息从生产者到队列的过程:

​		生产者发送消息到交换机,会返回一个confirmCallback;

​		交互机发送消息到队列,会返回一个returnCallback;

​		我们将利用这两个特性来控制消息的可靠性投递.

#### 1.生产者发送消息到交换机

​	处理步骤:

​	1.在配置文件中开启"确认模式";

```application.yml
server:
  port: 8899
spring:
  rabbitmq:
    host: 192.168.200.128
    port: 5672
    username: guest
    password: guest
    publisher-confirms: true  #为了确认消息从生产者成功发送到了指定交换机,需要开启这个 "确认模式"
```

​	2.在消息发送后接收回调函数并做以判断处理

```java
 @Test
    public void testSendMsg() {
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
```

#### 2.交换机路由消息到队列

处理步骤:

1.在配置文件中开启"回退模式";

```
publisher-returns: true  #为了确认消息从交换机成功发送到了指定队列,需要开启这个 "回退模式"
```

2.设置交互机的处理模式:

消息从交互机发送到队列失败时才会执行 returnCallback.

```java
a.如果消息没有成功路由到队列,则消息丢弃(默认方式,不需要设置)
b.如果消息没有成功路由到队列,则消息返回给交互机(需要在代码中设置)
	rabbitTemplate.setMandatory(true);
```

3.接收ReturnCallback函数(只有当消息由交互机路由到队列失败后才会执行)

```java
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
    public void testSendMsgReturn() {
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
                System.out.println("消息有交换机-->队列路由失败...");
                System.out.println("失败的原因是:"+s);
                System.out.println("消息是:"+message.toString());
            }
        });
        //1.发送消息
        rabbitTemplate.convertAndSend(RabbitMqConfig.EXCHANGE_TOPIC_ONE, "test02.demo111", "我是中国人,啦啦啦啦...");

    }
```

![image-20200422214019866](C:\Users\Administrator\AppData\Roaming\Typora\typora-user-images\image-20200422214019866.png)

### 二.消息从队列到消费者的过程:

#### consumer Ack 机制:

![image-20200422214926124](C:\Users\Administrator\AppData\Roaming\Typora\typora-user-images\image-20200422214926124.png)

![image-20200422220003809](C:\Users\Administrator\AppData\Roaming\Typora\typora-user-images\image-20200422220003809.png)

实现步骤:

1..配置文件中设置手动处理模式:

​		acknowledge-mode: manual

 2.让自定义的监听器实现ChannelAwareMessageListener接口;

3.用注解显示申明监听哪个/些队列

 4.如果消息成功处理,则调用channel的basicAck()方法进行签收;

 5.如果消息处理失败,则调用channel的basicNack()方法进行拒绝签收;

```java
package com.zhang.rabbitmq.listener;

import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
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
 * 2.让自定义的监听器实现ChannelAwareMessageListener接口
 * 3.用注解显示申明监听哪个/些队列
 * 4.如果消息成功处理,则调用channel的basicAck()方法进行签收;
 * 5.如果消息处理失败,则调用channel的basicNack()方法进行拒绝签收;
 */
@Component
public class AckListener implements ChannelAwareMessageListener {

    @Override
    public void onMessage(Message message, Channel channel) throws Exception {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            //1.接收转化消息
            System.out.println(new String(message.getBody()));

            //2.利用消息进行业务处理
            System.out.println("处理业务逻辑");
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

```

#### consumer Qos 机制:

1.qos机制指的是:消费端的限流,即消费端每次从mq中拉取多少条消息进行消费

实现步骤:

```java
1.确保消费者的ack机制为手动签收模式,(在配置文件中配置) 
	acknowledge-mode: manual
2.配置消费者每次拉取消息的数量的属性(在配置文件中配置)  
	prefetch: 自己根据服务器性能设定的数值
```

注意:这一步主要是配置+测试,没有代码.

### 三.死信队列

#### 1.死信队列的解释:

​	也叫死信交换机,指的是:mq中的普通消息由于某些原因变为死信消息时,这些死信消息就会进入到死信队列(即死信交换机)中,然后经由死信交换机路由到别的队列中.

![image-20200422224539812](C:\Users\Administrator\AppData\Roaming\Typora\typora-user-images\image-20200422224539812.png)

#### 2.死信队列出现的原因:

![image-20200422224658802](C:\Users\Administrator\AppData\Roaming\Typora\typora-user-images\image-20200422224658802.png)

#### 3.普通队列如何绑定死信交换机:

​	1.给队列设置以下两个参数:

![image-20200422225143175](C:\Users\Administrator\AppData\Roaming\Typora\typora-user-images\image-20200422225143175.png)

4.代码:参考springboot-rabbitmq项目中rabbitmqconfig配置类中申明交换机和队列的配置.或者自己利用图形化界面创建死信队列,交互机以及绑定关系.

### 四.延迟队列

#### 1.理解:

rabbitmq并没有单纯的消息延迟功能,我们可以用 ttl(过期时间)+死信队列来实现消息延迟的功能.

#### 2.使用场景举例:



### 五.日志消息追踪功能:

避免消息丢失,但会严重影响性能.具体使用时可以查看本节视频.

