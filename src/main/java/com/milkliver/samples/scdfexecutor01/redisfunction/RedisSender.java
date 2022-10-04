package com.milkliver.samples.scdfexecutor01.redisfunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.milkliver.samples.scdfexecutor01.utils.MsgSender;

@Component
public class RedisSender implements MsgSender {

	private static final Logger log = LoggerFactory.getLogger(RedisSender.class);

	@Value("${scdf.redis.key-prefix:}")
	String scdfRedisKeyPrefix;

	@Value("${scdf.redis.use-key-prefix:false}")
	Boolean scdfRedisUseKeyPrefix;

	@Value("${debug:false}")
	Boolean debugStatus;

	@Autowired
	RedisTemplate<String, String> redisTemplate;

	public void send(String message, String taskid) {
		log.info(this.getClass().getName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName()
				+ " taskid: " + taskid + " message: " + message + " ...");

		try {

			if (scdfRedisUseKeyPrefix) {
				redisTemplate.opsForValue().set(scdfRedisKeyPrefix + taskid, message);
			} else {
				redisTemplate.opsForValue().set(taskid, message);
			}

			if (debugStatus) {
				read(taskid);
			}

		} catch (Exception e) {
			log.error(e.getMessage());
			for (StackTraceElement elem : e.getStackTrace()) {
				log.error(elem.toString());
			}
		}

		log.info(this.getClass().getName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName()
				+ " taskid: " + taskid + " message: " + message + " finish");
	}

	public void read(String taskid) {
		log.info(this.getClass().getName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + " ...");

		try {
			String res = "";
			if (scdfRedisUseKeyPrefix) {
				res = redisTemplate.opsForValue().get(scdfRedisKeyPrefix + taskid);
				log.info("read redis key: " + scdfRedisKeyPrefix + taskid + " value: " + res);
			} else {
				res = redisTemplate.opsForValue().get(taskid);
				log.info("read redis key: " + taskid + " value: " + res);
			}

		} catch (Exception e) {
			log.error(e.getMessage());
			for (StackTraceElement elem : e.getStackTrace()) {
				log.error(elem.toString());
			}
		}

		log.info(this.getClass().getName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName()
				+ " finish");
	}

}
