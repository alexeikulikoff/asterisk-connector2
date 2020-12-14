package com.mibs.asterisk.web.events;

import org.apache.logging.log4j.LogManager;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class QueueCallerAbandonEvent implements AsteriskEvent {
	private static final org.apache.logging.log4j.Logger logger = LogManager
			.getLogger(QueueCallerAbandonEvent.class.getName());
	private String privilege;
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
	private String originalposition;
	private String holdtime;

	@Override
	public void execute(SimpMessagingTemplate template) {

		template.convertAndSend("/abandon", this);

	}

}
