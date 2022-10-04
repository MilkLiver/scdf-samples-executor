package com.milkliver.samples.scdfexecutor01.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class MsgOps {

	private static final Logger log = LoggerFactory.getLogger(MsgOps.class);

	@Autowired
	MsgSender msgSender;

	public void send(String message, String taskid) {
		log.info(this.getClass().getName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName()
				+ " taskid: " + taskid + " message: " + message + " ...");

		try {

			msgSender.send(message, taskid);

		} catch (Exception e) {
			log.error(e.getMessage());
			for (StackTraceElement elem : e.getStackTrace()) {
				log.error(elem.toString());
			}
		}

		log.info(this.getClass().getName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName()
				+ " taskid: " + taskid + " message: " + message + " finish");
	}
}
