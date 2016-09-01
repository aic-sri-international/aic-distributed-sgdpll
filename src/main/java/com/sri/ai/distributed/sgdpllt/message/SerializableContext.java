package com.sri.ai.distributed.sgdpllt.message;

import java.io.Serializable;

import com.sri.ai.grinder.sgdpllt.api.Context;

public class SerializableContext implements Serializable {
	private static final long serialVersionUID = 1L;

	// Is serializable??
	private Context localContext;
	
	public SerializableContext(Context localContext) {
		this.localContext = localContext;
	}
	
	public Context getLocalContext() {
		if (localContext == null) {
			// TODO - add support for context serialization
			throw new UnsupportedOperationException("TODO-add support for context serialization.");
		}
		return localContext;
	}
}
