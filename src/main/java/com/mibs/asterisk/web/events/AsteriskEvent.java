package com.mibs.asterisk.web.events;

import org.springframework.messaging.simp.SimpMessagingTemplate;

public interface AsteriskEvent {

	public void execute(SimpMessagingTemplate template);
}
