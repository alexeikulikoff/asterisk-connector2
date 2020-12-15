package com.mibs.asterisk.web.events;

import org.springframework.messaging.simp.SimpMessagingTemplate;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class QueueCallerJoinEvent implements AsteriskEvent {
	private String channel;
	private String channelstate;
	private String channelstatedesc;
	private String calleridnum;
	private String calleridname;
	private String connectedlinenum;
	private String connectedlinename;
	private String language;
	private String accountcode;
	private String context;
	private String exten;
	private String priority;
	private String uniqueid;
	private String linkedid;
	private String queue;
	private String position;
	private String count;

	@Override
	public void execute(SimpMessagingTemplate template) {

		template.convertAndSend("/join", this);

	}

}
