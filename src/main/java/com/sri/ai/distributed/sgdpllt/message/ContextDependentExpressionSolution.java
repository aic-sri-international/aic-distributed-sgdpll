package com.sri.ai.distributed.sgdpllt.message;

import java.io.Serializable;

import com.sri.ai.expresso.api.Expression;
import com.sri.ai.expresso.helper.Expressions;

// NOTE: Immutable
public class ContextDependentExpressionSolution implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public final String value;
	
	// DON'T SERIALIZE
	private transient Expression localValue; 
	
	public ContextDependentExpressionSolution(Expression value) {
		this.value = value.toString();
	}

	public Expression getLocalValue() {
		if (localValue == null) {
			localValue = Expressions.parse(value);
		}
		return localValue;
	}
}