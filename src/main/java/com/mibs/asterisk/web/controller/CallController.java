package com.mibs.asterisk.web.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;

import com.mibs.asterisk.web.AppConfig;
import com.mibs.asterisk.web.AsteriskListener;

public class CallController {
	private static final Logger logger = LogManager.getLogger(CallController.class.getName());
	private final Pattern pt = Pattern.compile("SIP/\\d+");

	@Autowired
	private AsteriskListener listener;

	@Autowired
	private AppConfig config;

	@GetMapping("/init")
	public InitQueue init() throws IOException {

		QueueContents content = getQueueContents();

		return new InitQueue("hello: " + content.toString());

	}

	private void doCommand(String queue) throws IOException {
		listener.getConnector().getPrintWriter()
				.write("Action: COMMAND\r\nActionID:12345\r\ncommand: queue show " + queue + "\r\n\r\n");
		listener.getConnector().getPrintWriter().flush();
	}

	public QueueContents getQueueContents() throws IOException {

		BufferedReader reader = listener.getConnector().getReader();

		String connURL = "jdbc:mysql://" + config.getDbHost() + ":3306/" + config.getDbName()
				+ "?useUnicode=yes&characterEncoding=UTF-8";

		QueueContents content = new QueueContents();
		String sql = "select name from queues";
		try (Connection connect = DriverManager.getConnection(connURL, config.getDbUser(), config.getDbPassword());
				Statement statement = connect.createStatement();
				ResultSet rs = statement.executeQuery(sql)) {

			while (rs.next()) {
				String queue = rs.getString("name");
				CurrentQueue currentQueue = new CurrentQueue(queue);
				doCommand(queue);
				boolean memberFlag = false;
				int index = 0;
				for (String line = reader.readLine(); line != null; line = reader.readLine()) {

					System.out.println(line);

					if (line.contains("Members")) {
						memberFlag = true;
					}
					if (memberFlag) {
						if (!line.contains("Members:") & !line.contains("No Members") & !line.contains("No Callers")) {
							Matcher m = pt.matcher(line);
							if (m.find())
								currentQueue.addMember(m.group(0));
						}
					}
					if (line.contains("Callers") & memberFlag) {
						memberFlag = false;
						content.addQueueResponce(currentQueue);
					}
					if (line.contains("No Callers") | line.contains("Callers:"))
						break;
					if (index++ > 10)
						break;
				}

			}

		} catch (Exception e) {
			logger.error("Error while extractiong Queue Contents with message: " + e.getMessage());

		}
		return content;
	}

}
