package com.milkliver.samples.scdfexecutor01.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CommandTools {

	private static final Logger log = LoggerFactory.getLogger(CommandTools.class);

	final Base64.Decoder decoder = Base64.getDecoder();
	final Base64.Encoder encoder = Base64.getEncoder();

	Map<String, Object> resultInfoMap = new HashMap<String, Object>();

	public Map<String, Object> executeCommandLine(final String commandLine, final boolean printOutput,
			final boolean printError, final long timeout) throws Exception {

		Runtime runtime = Runtime.getRuntime();
		Process process = null;
		Worker worker = null;
		try {
			log.info("command: " + commandLine);
			log.info("timeout: " + String.valueOf(timeout));
			log.info("printOutput: " + String.valueOf(printOutput));
			log.info("printError: " + String.valueOf(printError));

			resultInfoMap.put("timeout", timeout);

			process = runtime.exec(commandLine);
			/* Set up process I/O. */
			worker = new Worker(process, printOutput, printError);
			worker.start();

			resultInfoMap.put("command", new String(encoder.encode(commandLine.getBytes())));

			worker.join(timeout);
			if (worker.exit == null) {
				resultInfoMap.put("status", false);
				throw new TimeoutException();
			}
		} catch (Exception e) {
			Map<String, Object> errorMap = new HashMap<String, Object>();

			log.error(e.getMessage());
			errorMap.put("message", new String(encoder.encode(e.getMessage().getBytes())));

			StringBuilder errorLogSb = new StringBuilder();
			for (StackTraceElement elem : e.getStackTrace()) {
				log.error(elem.toString());
				errorLogSb.append(elem.toString() + " ");
			}
			errorMap.put("stackTrace", new String(encoder.encode(errorLogSb.toString().getBytes())));
			resultInfoMap.put("error", errorMap);

			resultInfoMap.put("status", false);
			worker.interrupt();
			Thread.currentThread().interrupt();

		} finally {
			if (process != null) {
				process.destroy();
			}
			return resultInfoMap;
		}
	}

	private class Worker extends Thread {
		private final Process process;
		private Integer exit;
		private boolean printOutput;
		private boolean printError;

		private Worker(Process process, boolean printOutput, boolean printError) {
			this.process = process;
			this.printOutput = printOutput;
			this.printError = printError;
		}

		public void run() {
			try {
				exit = process.waitFor();
				log.info("exit: " + exit);

				resultInfoMap.put("exit", exit);

				Map<String, Object> executeMap = new HashMap<String, Object>();

				if (exit == 0) {
					log.info("task is success");
					resultInfoMap.put("status", true);
				} else {
					log.info("task is failed");
					resultInfoMap.put("status", false);

				}
				// print exec output log
				log.info("----------------print exec output----------------");
				executeMap.put("log", printExecuteMessage(process.getInputStream()));
				log.info("-------------------------------------------------");

				// print exec error log
				log.info("----------------print exec error----------------");
				executeMap.put("error", printExecuteMessage(process.getErrorStream()));
				log.info("-------------------------------------------------");

				resultInfoMap.put("output", executeMap);

			} catch (InterruptedException ignore) {
				return;
			}
		}
	}

	private String printExecuteMessage(InputStream resInputStream) {
		StringBuilder execCmdRes = new StringBuilder();

		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(resInputStream));
		String line;

		try {
			while ((line = bufferedReader.readLine()) != null) {
				execCmdRes.append(line);
				execCmdRes.append("\r\n");
			}
			log.info(execCmdRes.toString());
		} catch (Exception e) {
			log.error(e.getMessage());
			for (StackTraceElement elem : e.getStackTrace()) {
				log.error(elem.toString());
			}
		}
		return new String(encoder.encode(execCmdRes.toString().getBytes()));

	}
}
