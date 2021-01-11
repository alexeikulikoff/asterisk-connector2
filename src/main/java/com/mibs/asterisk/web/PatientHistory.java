package com.mibs.asterisk.web;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class PatientHistory implements java.io.Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String phone;
	private String name;
	private List<String> history = new ArrayList<>();

	private PatientHistory(String phone, String name) {
		this.phone = phone;
		this.name = name;
	}

	public static PatientHistory of(String phone, String name) {
		return new PatientHistory(phone, name);
	}

	public void addHistory(String ev) {
		history.add(ev);
	}

}
