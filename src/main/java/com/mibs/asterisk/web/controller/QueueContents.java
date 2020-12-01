package com.mibs.asterisk.web.controller;

import java.util.Set;
import java.util.TreeSet;

public class QueueContents {

	private Set<CurrentQueue> queues;;
	public QueueContents() {
		queues = new TreeSet<>();
	}
	public void addQueueResponce(CurrentQueue qr) {
		queues.add(qr);
	}
	@Override
	public String toString() {
		return "Queues [" + queues + "]";
	}
	public boolean isContain(String queueName, String memberName) {
		for(CurrentQueue qr : queues ) {
			if (qr.getQueue().equals(queueName)) {
				return qr.isContainMember(memberName);
			}
		}
		return false;
	}
	public Set<CurrentQueue> getQueues() {
		return queues;
	}
	
}
