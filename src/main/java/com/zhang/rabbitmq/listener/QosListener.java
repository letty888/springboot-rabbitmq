package com.zhang.rabbitmq.listener;

import org.springframework.stereotype.Component;

/**
 * @author zhang
 * @version 1.0
 * @date 2020/4/22 22:26
 * 消费者限流机制:
 * 前提:
 * 1.确保消费者的ack机制为手动签收模式,(在配置文件中配置) acknowledge-mode: manual
 * 2.配置消费者每次拉取消息的数量的属性(在配置文件中配置)  prefetch: 1
 */
@Component
public class QosListener {
}
