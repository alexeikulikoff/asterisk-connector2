package com.mibs.asterisk.web.events;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class QueueMemberRemovedEvent implements AsteriskEvent {
	private String queue;
	private String location;
	private String membername;
	private String queueid;

	@Override
	public void execute() {
		// TODO Auto-generated method stub

	}

}
