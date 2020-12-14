package com.mibs.asterisk.web.events;

import org.springframework.messaging.simp.SimpMessagingTemplate;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class BridgeEvent implements AsteriskEvent {
	private String bridgestate;
	private String bridgetype;
	private String channel1;
	private String channel2;
	private String uniqueid1;
	private String uniqueid2;
	private String callerid1;
	private String callerid2;
	private String customer;

	@Override
	public void execute(SimpMessagingTemplate template) {

		template.convertAndSend("/bridge", this);

	}

}
