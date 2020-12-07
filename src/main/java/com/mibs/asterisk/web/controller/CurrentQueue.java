package com.mibs.asterisk.web.controller;

import java.util.Set;
import java.util.TreeSet;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class CurrentQueue implements Comparable<CurrentQueue> {

	private String queue;
	private Set<String> members;
	private int callers;

	public CurrentQueue(String q) {
		queue = q;
		members = new TreeSet<>();
	}

	public void addMember(String m) {
		members.add(m);
	}

	public boolean isContainMember(String memberName) {
		for (String s : members) {
			if (s.equals(memberName))
				return true;
		}
		return false;
	}

	@Override
	public int compareTo(CurrentQueue o) {
		return queue.compareTo(o.queue);
	}

}
