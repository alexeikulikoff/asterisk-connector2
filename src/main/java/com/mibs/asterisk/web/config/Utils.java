package com.mibs.asterisk.web.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mibs.asterisk.web.AppConfig;

public class Utils {

	private static AppConfig config;

	private static final Logger logger = LogManager.getLogger(Utils.class.getName());

	public static void init(AppConfig conf) {

		config = conf;
	}

	private static Optional<String> getAgentName(Long queueid, Long peerid) {
		String connURL = "jdbc:mysql://" + config.getDbHost() + ":3306/" + config.getDbName()
				+ "?useUnicode=yes&characterEncoding=UTF-8";
		String result = null;
		try (Connection connect = DriverManager.getConnection(connURL, config.getDbUser(), config.getDbPassword());
				Statement statement = connect.createStatement()) {
			String sql = "SELECT name  FROM agents ag  inner join " + " (select agentid  from members where queueid="
					+ queueid + " and peerid=" + peerid + "  and event='ADDMEMBER' ORDER BY ID DESC LIMIT 1) mb "
					+ " on ag.id = mb.agentid";

			ResultSet rs = statement.executeQuery(sql);
			rs.next();
			result = rs.getString("name");
			rs.close();
		} catch (Exception e) {
			logger.error(e.getMessage());

		}
		return Optional.ofNullable(result);
	}

	public static Optional<String> getAgentName(String queue, String phone) {

		Optional<Long> optA = null;
		if (phone != null && phone.length() > 0) {
			optA = getPeerIdByName(phone);
		} else {
			return Optional.empty();
		}
		Optional<Long> optB = null;
		if (queue != null && queue.length() > 0) {
			optB = getQueueIdByName(queue);

		} else {
			return Optional.empty();
		}
		if (optA.isPresent() && optB.isPresent()) {
			Optional<String> result = getAgentName(optB.get(), optA.get());
			return result;
		} else {
			return Optional.empty();
		}

	}

	private static Optional<Long> getPeerIdByName(String name) {

		String connURL = "jdbc:mysql://" + config.getDbHost() + ":3306/" + config.getDbName()
				+ "?useUnicode=yes&characterEncoding=UTF-8";
		Long result = null;
		try (Connection connect = DriverManager.getConnection(connURL, config.getDbUser(), config.getDbPassword());
				Statement statement = connect.createStatement()) {
			String sql = "select id from peers where name = '" + name.toUpperCase() + "'";
			ResultSet rs = statement.executeQuery(sql);
			if (rs.next())
				result = rs.getLong("id");
			rs.close();
		} catch (Exception e) {
			logger.error("Error in getPeerIdByNam :" + e.getMessage());
		}
		return Optional.ofNullable(result);
	}

	private static Optional<Long> getQueueIdByName(String name) {
		String connURL = "jdbc:mysql://" + config.getDbHost() + ":3306/" + config.getDbName()
				+ "?useUnicode=yes&characterEncoding=UTF-8";
		Long result = null;
		try (Connection connect = DriverManager.getConnection(connURL, config.getDbUser(), config.getDbPassword());
				Statement statement = connect.createStatement()) {
			String sql = "select id from queues where name = '" + name + "'";
			ResultSet rs = statement.executeQuery(sql);
			if (rs.next())
				result = rs.getLong("id");
			rs.close();
		} catch (Exception e) {
			logger.error("Error in getQueueIdByName " + e.getMessage());
		}
		return Optional.ofNullable(result);
	}

}
