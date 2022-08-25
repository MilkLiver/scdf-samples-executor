package com.milkliver.samples.scdfexecutor01.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;

@Service
public class MessageProducer {

	private static final Logger log = LoggerFactory.getLogger(MessageProducer.class);

	@Autowired
	private KafkaTemplate kafkaTemplate;

	@Value("${spring.kafka.template.default-topic:#{null}}")
	String springKafkaTemplateDefaultTopic;

	public void send(String message) {

		log.info("===============================kafka send message===============================");
		if (springKafkaTemplateDefaultTopic == null || springKafkaTemplateDefaultTopic.trim().equals("")) {
			log.info("kafka is not set topic");
		} else {
			log.info(this.getClass().toString() + " message: " + message + " send ...");

			ListenableFuture future = kafkaTemplate.send(springKafkaTemplateDefaultTopic, message);
			future.addCallback(o -> log.info("send-Message Success：" + message),
					throwable -> log.info("send-Message Fail：" + message));

			log.info(this.getClass().toString() + " message: " + message + " send finish");
		}
		log.info("=============================kafka send message end=============================");
	}
}
