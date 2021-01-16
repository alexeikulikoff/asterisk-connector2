package com.mibs.asterisk.web;

/**
 * 
 *  Пример загрузки истории для одного пациента в виде сериализованного объекта
 * 
 * 
 */

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.SerializationUtils;

import redis.clients.jedis.Jedis;

@SpringBootTest
class ApplicationTests {

	@Autowired
	private AppConfig config;

	private static final UUID id1 = UUID.randomUUID();
	private static final UUID id2 = UUID.randomUUID();
	private static final UUID id3 = UUID.randomUUID();
	private static final UUID id4 = UUID.randomUUID();
	private static final UUID id5 = UUID.randomUUID();

	private static final String r1 = "МРТ позвоночника";
	private static final String r2 = "МРТ головы";
	private static final String r3 = "МРТ шеи";
	private static final String r4 = "МРТ суставов";
	private static final String r5 = "МРТ позвоночника";

	private static final String patient1 = "Петров Сергей Иванович";
	private static final String patient2 = "Соколов Федор Иванович";

	private static final String phone = "89211234567";

	@Test
	void loadHistoryTest() {

		MedicalResearch medicalResearch1 = MedicalResearch.of(patient1);
		Map<UUID, String> map1 = new HashMap<>();
		map1.put(id1, r1);
		map1.put(id2, r2);
		map1.put(id3, r3);
		medicalResearch1.setResearch(map1);

		MedicalResearch medicalResearch2 = MedicalResearch.of(patient2);
		Map<UUID, String> map2 = new HashMap<>();
		map2.put(id4, r4);
		map2.put(id5, r5);
		medicalResearch2.setResearch(map2);

		PatientHistory ph = PatientHistory.of(phone).addResearch(medicalResearch1).addResearch(medicalResearch2);

		// Jedis jedis = new Jedis(config.getRedisHost());
		// jedis.auth(config.getRedisPssword());

		Jedis jedis = new Jedis("172.16.255.10");
		jedis.auth("kukla");

		byte[] rc = SerializationUtils.serialize(ph);

		jedis.set(phone.getBytes(), rc);

		byte[] rs = jedis.get(phone.getBytes());

		PatientHistory test = (PatientHistory) SerializationUtils.deserialize(rs);

		assertEquals(phone, test.getPhone());
		assertEquals(test.getMedicalResearches().get(0).getPatinentName(), patient1);
		assertEquals(test.getMedicalResearches().get(0).getResearch().get(id1), r1);
		assertEquals(test.getMedicalResearches().get(0).getResearch().get(id2), r2);
		assertEquals(test.getMedicalResearches().get(0).getResearch().get(id3), r3);

		assertEquals(test.getMedicalResearches().get(1).getResearch().get(id4), r4);
		assertEquals(test.getMedicalResearches().get(1).getResearch().get(id5), r5);

	}

}
