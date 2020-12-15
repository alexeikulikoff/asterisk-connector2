package com.mibs.asterisk.web.events;

import java.util.Optional;

import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.mibs.asterisk.web.config.Utils;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class QueueMemberAddedEvent implements AsteriskEvent {
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
	@Getter(AccessLevel.NONE)
	private String agentname;
	private String queueid;

	public String getAgentname() {
		Optional<String> op = Utils.getAgentName(queue, membername);
		return op.isPresent() ? op.get() : "Unknown";
	}

	@Override
	public void execute(SimpMessagingTemplate template) {

		template.convertAndSend("/add", this);

	}

}
