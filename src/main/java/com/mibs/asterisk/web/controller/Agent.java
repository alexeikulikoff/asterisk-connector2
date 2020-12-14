package com.mibs.asterisk.web.controller;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Agent implements Comparable<Agent> {

	private String number;
	private String name;
	private String state;

	@Override
	public int compareTo(Agent o) {

		return o.number.compareTo(number);
	}

}
