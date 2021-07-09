package com.example.mod;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * An example mod as filler for a template.
 *
 * @since 0.0.0
 **/
public class Example {
	public static final Logger logger = LogManager.getLogger("Example");

	public static void init() {
		logger.info("Hi, this is an example mod!");
	}
}
