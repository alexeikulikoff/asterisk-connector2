package com.mibs.asterisk.web.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CallController {

	@GetMapping("/init")
	public InitQueue init() {
		return new InitQueue("hello");
	}

}
