package com.revature.web;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class DemoServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final ObjectMapper mapper = new ObjectMapper();
	
	static {
		// This is a static initializer
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("application/json");
		resp.getOutputStream().write(mapper.writeValueAsBytes(Collections.singletonMap("message", "Hello, World! It is " + LocalDateTime.now())));
	}

}
