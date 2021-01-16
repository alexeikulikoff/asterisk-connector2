package com.mibs.asterisk.web;

/**
 * 
 *  Класс-контейнер для истории пациента
 * 
 * 
 */

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class PatientHistory implements java.io.Serializable {

	private static final long serialVersionUID = 1L;
	private String phone;

	private List<MedicalResearch> medicalResearches = new ArrayList<>();

	private PatientHistory(String phone) {
		this.phone = phone;
	}

	public static PatientHistory of(String phone) {

		return new PatientHistory(phone);
	}

	public PatientHistory addResearch(MedicalResearch medicalResearch) {

		medicalResearches.add(medicalResearch);

		return this;
	}

}
