package com.sri.ai.distributed.sgdpllt.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import akka.event.LoggingAdapter;

public class TestSerialize {

	public static final boolean SERIALIZE_ALL_MESSAGES = false;
	public static final boolean LOG_SERIALIZED_MESSAGE_SIZES = false;
	
	public static Object serializeMessage(Object msg, LoggingAdapter log) {
		Object result = null;
		
		if (SERIALIZE_ALL_MESSAGES) {
			try {
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				ObjectOutputStream oos = new ObjectOutputStream(bos);
		        oos.writeObject(msg);
		        oos.close();
		        
		        byte[] msgBytes = bos.toByteArray();
		        
		        if (LOG_SERIALIZED_MESSAGE_SIZES) {
		        	log.debug("Serialize {}, #bytes={}", msg.getClass().getSimpleName(), msgBytes.length);
		        }
		        
		        ByteArrayInputStream bis = new ByteArrayInputStream(msgBytes);
		        ObjectInputStream ois = new ObjectInputStream(bis);
		        result = ois.readObject();
		        ois.close();
			}
			catch (Exception ex) {
				System.err.println("Exception thrown in TestSerializer");
				ex.printStackTrace();
				throw new RuntimeException(ex);
			}
		}
		else {
			result = msg;
		}
		
		return result;
	}
}
