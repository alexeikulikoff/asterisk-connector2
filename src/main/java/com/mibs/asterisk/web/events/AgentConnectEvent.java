package com.mibs.asterisk.web.events;

import org.springframework.messaging.simp.SimpMessagingTemplate;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class AgentConnectEvent implements AsteriskEvent {

	private String queue;
	private String member;
	private String memberName;
	private String ringTime;
	private String holdTime;
	private String uniqueid;
	private String channel;

	@Override
	public void execute(SimpMessagingTemplate template) {

		template.convertAndSend("/connect", this);

	}

}
