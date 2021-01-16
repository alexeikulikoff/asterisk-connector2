package com.mibs.asterisk.web;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MedicalResearch implements java.io.Serializable {

	private static final long serialVersionUID = 1L;
	private String patinentName;
	private Map<UUID, String> research = new HashMap<>();

	private MedicalResearch(String patinentName) {
		super();
		this.patinentName = patinentName;
	}

	public static MedicalResearch of(String patinentName) {

		return new MedicalResearch(patinentName);
	}

}
