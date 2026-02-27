package br.com.fightConnect.infrastructure.configurations;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

	public static final String FILA_NOTIFICACAO = "notificacao.automatica";

	@Bean
	public Queue notificacaoAutomaticaQueue() {
		return new Queue(FILA_NOTIFICACAO, true);
	}

	@Bean
	public MessageConverter jackson2MessageConverter() {
		return new Jackson2JsonMessageConverter();
	}

	// Configura o listener para converter JSON em Map automaticamente
	@Bean
	public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory,
			MessageConverter messageConverter) {
		SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
		factory.setConnectionFactory(connectionFactory);
		factory.setMessageConverter(messageConverter);
		return factory;
	}
	@Bean
	public TopicExchange exchangeEventsFightconnect(
			@org.springframework.beans.factory.annotation.Value("${app.rabbit.exchange}")
			String exchangeName
	) {
		return new TopicExchange(exchangeName, true, false);
	}

	@Bean
	public TopicExchange exchangeSyncFightConnect() {
		return new TopicExchange("sync.fightconnect", true, false);
	}
	@Bean
	public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
	    RabbitTemplate template = new RabbitTemplate(connectionFactory);
	    template.setMessageConverter(messageConverter); // 👈 ESSENCIAL: garante envio como JSON
	    return template;
	}
}
