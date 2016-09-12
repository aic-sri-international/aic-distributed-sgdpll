package com.sri.ai.distributed.sgdpllt.util;

import java.util.concurrent.TimeUnit;

import akka.util.Timeout;

public class AkkaUtil {
	/// TODO - make configurable
	private static final Timeout _defaultTimeout = new Timeout(24, TimeUnit.HOURS);
	
	public static Timeout getDefaultTimeout() {
		return _defaultTimeout;
	}
}
