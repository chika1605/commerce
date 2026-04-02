package com.pro.events.config;

import com.pro.events.model.EventTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic eventOrderCreatedTopic() {
        return TopicBuilder.name(EventTopics.ORDER_CREATED)
                .partitions(1)
                .replicas(1)
                .build();
    }

}
