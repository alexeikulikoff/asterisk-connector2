package com.mibs.asterisk.web.controller;

import org.springframework.stereotype.Controller;

@Controller
public class PrintTestController {
	public static void print(String input) {
		System.out.println(input);
	}
}
