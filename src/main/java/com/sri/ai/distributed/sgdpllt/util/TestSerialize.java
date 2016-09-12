package com.sri.ai.distributed.sgdpllt.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import akka.event.LoggingAdapter;

public class TestSerialize {

	// NOTE: required to be true currently for logic to work correctly. 
	// This is because cloning is not sufficient as only shallow copies
	// are made at this causes the DistributedTheory object to be updated
	// incorrectly across nested calls. Do not want to adjust aic-expresso
	// classes clone() methods to support as this is only a prototype
	// approach and in practice we will want a much cleaner explicit mechanism
	// for distributing computation.
	public static final boolean SERIALIZE_ALL_MESSAGES = true; 
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
