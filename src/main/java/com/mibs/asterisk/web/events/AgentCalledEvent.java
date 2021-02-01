package com.mibs.asterisk.web.events;

import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.mibs.asterisk.web.AsteriskListener;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class AgentCalledEvent implements AsteriskEvent {
	private String privilege;
	private String channel;
	private String channelstate;
	private String channelstateDesc;
	private String callerIdnum;
	private String callerIdname;
	private String connectedlinenum;
	private String connectedlinename;
	private String language;
	private String accountcode;
	private String context;
	private String exten;
	private String priority;
	private String uniqueid;
	private String linkedid;
	private String destchannel;
	private String destchannelstate;
	private String destchannelstatedesc;
	private String destcallerIdnum;
	private String destcallerIdname;
	private String destconnectedlinenum;
	private String destconnectedlinename;
	private String destlanguage;
	private String destaccountcode;
	private String destcontext;
	private String destexten;
	private String destpriority;
	private String destuniqueid;
	private String destlinkedid;
	private String queue;
	// private String interface;
	private String mmembermame;

	@Override
	public void execute(SimpMessagingTemplate template) {
		template.convertAndSend("/call", this);

		System.out.println(this);
		AsteriskListener.publish(this);

	}

}
