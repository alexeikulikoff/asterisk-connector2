package com.mibs.asterisk.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.SerializationUtils;

import com.mibs.asterisk.web.controller.PrintTestController;

import redis.clients.jedis.Jedis;

@SpringBootTest
class ApplicationTests {

	@Autowired
	private AppConfig config;

	@Test
	void loadHistory() {

		String phone = "9608778822";
		String name = "Marco Polo Rodriges";

		PatientHistory ph = PatientHistory.of(phone, name);
		ph.addHistory("A personal medical history may include information about allergies");
		ph.addHistory(
				"It may also include information about medicines taken and health habits, such as diet and exercise.");

		PrintTestController.print(config.getRedisHost());

		Jedis jedis = new Jedis(config.getRedisHost());
		jedis.auth(config.getRedisPssword());

		byte[] rc = SerializationUtils.serialize(ph);

		jedis.set(phone.getBytes(), rc);

		byte[] rs = jedis.get(phone.getBytes());

		PatientHistory test = (PatientHistory) SerializationUtils.deserialize(rs);

		PrintTestController.print(test.toString());

		assertEquals(phone, test.getPhone());
		assertEquals(name, test.getName());
		assertEquals("A personal medical history may include information about allergies", test.getHistory().get(0));

	}

}
