package com.milkliver.samples.scdfexecutor01.task;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.milkliver.samples.scdfexecutor01.kafka.MessageProducer;
import com.milkliver.samples.scdfexecutor01.utils.SendRequest;

@Component
@EnableTask
public class MainTask {

	private static final Logger log = LoggerFactory.getLogger(MainTask.class);

	final Base64.Decoder decoder = Base64.getDecoder();
	final Base64.Encoder encoder = Base64.getEncoder();

	@Value("${scdf.server.runtask.api.request.url:#{null}}")
	String scdfServerRuntaskApiRequestUrl;

	@Value("${scdf.server.runtask.api.request.hostname:#{null}}")
	String scdfServerRuntaskApiRequestHostname;

	@Value("${scdf.server.runtask.api.request.method:GET}")
	String scdfServerRuntaskApiRequestMethod;

	@Value("${scdf.server.runtask.api.request.connect-time-out:2000}")
	int scdfServerRuntaskApiRequestConnectTimeOut;

	@Value("${scdf.server.runtask.api.request.read-time-out:2000}")
	int scdfServerRuntaskApiRequestReadTimeOut;

	@Value("${scdf.server.runtask.api.request.enable-https:false}")
	Boolean scdfServerRuntaskApiRequestEnableHttps;

	@Value("${system.command}")
	String systemCommandBase64;

	@Autowired
	SendRequest sendRequest;

	@Value("${spring.cloud.task.executionid:#{null}}")
	String taskid;

	@Autowired
	MessageProducer messageProducer;

	@Bean
	public CommandLineRunner commandLineRunner() {
		return args -> {

			log.info("SCDF executor CommandLineRunner ...");
			if (taskid != null) {
				log.info("taskid: " + String.valueOf(taskid) + " is running ...");
			} else {
				log.info("taskid: null is running ...");
			}

			try {
				log.info("CommandBase64: " + systemCommandBase64);
				String systemCommand = new String(decoder.decode(systemCommandBase64));
				log.info("Command: " + systemCommand);
				log.info("========================start========================");

				Process process = Runtime.getRuntime().exec(systemCommand);
//				============================================================================================
				StringBuilder execCmdRes = new StringBuilder();

				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
				String line;
				while ((line = bufferedReader.readLine()) != null) {
					execCmdRes.append(line);
					execCmdRes.append("\r\n");
				}
//				============================================================================================

				log.info(execCmdRes.toString());

				log.info("waitFor: " + String.valueOf(process.waitFor()));

				log.info("=========================end=========================");

				Map<String, Object> taskExecuteResMsgMap = new HashMap<String, Object>();
				Map<String, Object> batchInfoMap = new HashMap<String, Object>();
				boolean taskExecuteStatus = false;

				if (process.waitFor() != 0) {
					log.info("task is failed");
				} else {
					taskExecuteStatus = true;
//					sendResultApi();
					log.info("task is success");
				}

				// create json string
				String execCmdResLogsBase64 = new String(encoder.encode(execCmdRes.toString().getBytes()));

				taskExecuteResMsgMap.put("status", taskExecuteStatus);

				batchInfoMap.put("logs", execCmdResLogsBase64);

				batchInfoMap.put("command", systemCommandBase64);
				taskExecuteResMsgMap.put("batch", batchInfoMap);

				if (taskid != null) {
					taskExecuteResMsgMap.put("taskid", taskExecuteStatus);
					log.info("taskid: " + String.valueOf(taskid) + " is running ...");
				} else {
					taskExecuteResMsgMap.put("taskid", "-1");
					log.info("taskid: null is running ...");
				}
				ObjectMapper taskExecuteResMsgJson = new ObjectMapper();
				StringBuilder taskExecuteResMsgJsonStrSb = new StringBuilder();

				taskExecuteResMsgJsonStrSb.append(taskExecuteResMsgJson.writeValueAsString(taskExecuteResMsgMap));

				// send json to kafka
				log.info("send to kafka json: " + taskExecuteResMsgJsonStrSb.toString());
				try {
					messageProducer.send(taskExecuteResMsgJsonStrSb.toString());
				} catch (Exception e) {
					log.error(e.getMessage());
					for (StackTraceElement elem : e.getStackTrace()) {
						log.error(elem.toString());
					}
				}

				process.destroy();

			} catch (Exception e) {
				log.error(e.getMessage());
				for (StackTraceElement elem : e.getStackTrace()) {
					log.error(elem.toString());
				}
			}
			log.info("CommandLineRunner finish");

			if (taskid != null) {
				log.info("taskid: " + String.valueOf(taskid) + " is finished ...");
			} else {
				log.info("taskid: null is finished ...");
			}
			log.info("SCDF executor CommandLineRunner finish");
		};
	}

	public void sendResultApi() {

		Map<String, Object> jsonMap = new HashMap<String, Object>();
		jsonMap.put("message", taskid);

		Map<String, Object> sendSuccessMsgRes = new HashMap<String, Object>();
		if (scdfServerRuntaskApiRequestEnableHttps) {
			try {
				sendSuccessMsgRes = sendRequest.https(scdfServerRuntaskApiRequestUrl,
						scdfServerRuntaskApiRequestHostname, scdfServerRuntaskApiRequestMethod,
						scdfServerRuntaskApiRequestConnectTimeOut, scdfServerRuntaskApiRequestReadTimeOut, jsonMap);
			} catch (Exception e) {
				log.error(e.getMessage());
				for (StackTraceElement elem : e.getStackTrace()) {
					log.error(elem.toString());
				}
			}

		} else {
			try {
				sendSuccessMsgRes = sendRequest.http(scdfServerRuntaskApiRequestUrl,
						scdfServerRuntaskApiRequestHostname, scdfServerRuntaskApiRequestMethod,
						scdfServerRuntaskApiRequestConnectTimeOut, scdfServerRuntaskApiRequestReadTimeOut, jsonMap);
			} catch (Exception e) {
				log.error(e.getMessage());
				for (StackTraceElement elem : e.getStackTrace()) {
					log.error(elem.toString());
				}
			}
		}
		log.info("response content: " + String.valueOf(sendSuccessMsgRes.get("responseContent")));
	}

}
