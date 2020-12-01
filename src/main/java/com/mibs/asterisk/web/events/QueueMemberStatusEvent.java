package com.mibs.asterisk.web.events;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class QueueMemberStatusEvent implements AsteriskEvent {
	private String privilege;
	private String queue;
	private String location;
	private String membername;
	private String stateinterface;
	private String membership;
	private String penalty;
	private String callstaken;
	private String lastcall;
	private String status;
	private String paused;
	private String queueid;

	@Override
	public void execute() {
		// TODO Auto-generated method stub

	}

}