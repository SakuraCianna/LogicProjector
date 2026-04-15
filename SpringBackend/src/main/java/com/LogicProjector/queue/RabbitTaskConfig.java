package com.LogicProjector.queue;

import org.aopalliance.aop.Advice;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitTaskConfig {

    @Bean
    DirectExchange taskExchange(@Value("${pas.queue.exchange}") String exchangeName) {
        return new DirectExchange(exchangeName);
    }

    @Bean
    DirectExchange deadLetterExchange(@Value("${pas.queue.dead-letter-exchange}") String deadLetterExchangeName) {
        return new DirectExchange(deadLetterExchangeName);
    }

    @Bean
    Queue generationQueue() {
        return new Queue(RabbitTaskQueues.GENERATION_QUEUE, true);
    }

    @Bean
    Queue exportQueue() {
        return new Queue(RabbitTaskQueues.EXPORT_QUEUE, true);
    }

    @Bean
    Queue generationDeadLetterQueue() {
        return new Queue(RabbitTaskQueues.GENERATION_DLQ, true);
    }

    @Bean
    Queue exportDeadLetterQueue() {
        return new Queue(RabbitTaskQueues.EXPORT_DLQ, true);
    }

    @Bean
    Binding generationBinding(Queue generationQueue,
            DirectExchange taskExchange,
            @Value("${pas.queue.generation-routing-key}") String routingKey) {
        return BindingBuilder.bind(generationQueue).to(taskExchange).with(routingKey);
    }

    @Bean
    Binding exportBinding(Queue exportQueue,
            DirectExchange taskExchange,
            @Value("${pas.queue.export-routing-key}") String routingKey) {
        return BindingBuilder.bind(exportQueue).to(taskExchange).with(routingKey);
    }

    @Bean
    Binding generationDlqBinding(Queue generationDeadLetterQueue,
            DirectExchange deadLetterExchange,
            @Value("${pas.queue.generation-dlq-routing-key}") String routingKey) {
        return BindingBuilder.bind(generationDeadLetterQueue).to(deadLetterExchange).with(routingKey);
    }

    @Bean
    Binding exportDlqBinding(Queue exportDeadLetterQueue,
            DirectExchange deadLetterExchange,
            @Value("${pas.queue.export-dlq-routing-key}") String routingKey) {
        return BindingBuilder.bind(exportDeadLetterQueue).to(deadLetterExchange).with(routingKey);
    }

    @Bean
    Jackson2JsonMessageConverter rabbitMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
        rabbitAdmin.setAutoStartup(true);
        return rabbitAdmin;
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, Jackson2JsonMessageConverter converter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(converter);
        return rabbitTemplate;
    }

    @Bean
    SimpleRabbitListenerContainerFactory generationListenerContainerFactory(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter converter,
            @Qualifier("generationRetryAdvice") Advice retryAdvice,
            @Value("${spring.rabbitmq.listener.simple.auto-startup:true}") boolean autoStartup) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(converter);
        factory.setAdviceChain(retryAdvice);
        factory.setDefaultRequeueRejected(false);
        factory.setAutoStartup(autoStartup);
        return factory;
    }

    @Bean
    SimpleRabbitListenerContainerFactory exportListenerContainerFactory(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter converter,
            @Qualifier("exportRetryAdvice") Advice retryAdvice,
            @Value("${spring.rabbitmq.listener.simple.auto-startup:true}") boolean autoStartup) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(converter);
        factory.setAdviceChain(retryAdvice);
        factory.setDefaultRequeueRejected(false);
        factory.setAutoStartup(autoStartup);
        return factory;
    }

    @Bean
    Advice generationRetryAdvice(RabbitTemplate rabbitTemplate,
            @Value("${pas.queue.dead-letter-exchange}") String deadLetterExchange,
            @Value("${pas.queue.generation-dlq-routing-key}") String deadLetterRoutingKey) {
        return RetryInterceptorBuilder.stateless()
                .maxAttempts(3)
                .recoverer(new RepublishMessageRecoverer(rabbitTemplate, deadLetterExchange, deadLetterRoutingKey))
                .build();
    }

    @Bean
    Advice exportRetryAdvice(RabbitTemplate rabbitTemplate,
            @Value("${pas.queue.dead-letter-exchange}") String deadLetterExchange,
            @Value("${pas.queue.export-dlq-routing-key}") String deadLetterRoutingKey) {
        return RetryInterceptorBuilder.stateless()
                .maxAttempts(3)
                .recoverer(new RepublishMessageRecoverer(rabbitTemplate, deadLetterExchange, deadLetterRoutingKey))
                .build();
    }
}
