package com.sri.ai.distributed.sgdpllt.util;

import java.util.concurrent.TimeUnit;

import akka.util.Timeout;

public class AkkaUtil {
	/// TODO - make configurable
	private static final Timeout _defaultTimeout = new Timeout(3600, TimeUnit.SECONDS);
	
	public static Timeout getDefaultTimeout() {
		return _defaultTimeout;
	}
}
