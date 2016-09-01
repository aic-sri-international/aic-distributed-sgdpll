package com.sri.ai.distributed.sgdpllt.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class TestSerialize {

	public static final boolean SERIALIZE_ALL_MESSAGES = true;
	
	public static Object serializeMessage(Object msg) {
		Object result = null;
		
		if (SERIALIZE_ALL_MESSAGES) {
			try {
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				ObjectOutputStream oos = new ObjectOutputStream(bos);
		        oos.writeObject(msg);
		        oos.close();
		        
		        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
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
