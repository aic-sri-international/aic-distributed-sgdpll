package com.sri.ai.distributed.sgdpllt.message;

import java.io.Serializable;

import com.sri.ai.expresso.api.Expression;
import com.sri.ai.grinder.sgdpllt.api.Context;
import com.sri.ai.grinder.sgdpllt.api.ContextDependentProblemStepSolver;

// NOTE: Immutable
public class ContextDependentExpressionProblem implements Serializable {	
	private static final long serialVersionUID = 1L;
	
	// DON'T SERIALIZE
	
	public ContextDependentExpressionProblem(ContextDependentExpressionProblem parentProblem, ContextDependentProblemStepSolver<Expression> stepSolver, Context context) {
		// TODO
	}
	
	public ContextDependentProblemStepSolver<Expression> getLocalStepSolver() {
		return null; // TODO
	}
	
	public Context getLocalContext() {
		return null; // TODO
	}
}
