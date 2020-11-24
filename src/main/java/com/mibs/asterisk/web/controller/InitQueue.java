package com.mibs.asterisk.web.controller;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InitQueue {

	private String queueName;

	public InitQueue(String queueName) {
		super();
		this.queueName = queueName;
	}

}
