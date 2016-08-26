package com.sri.ai.distributed.sgdpllt.message;

import com.sri.ai.expresso.api.Expression;
import com.sri.ai.grinder.sgdpllt.api.Context;
import com.sri.ai.grinder.sgdpllt.api.ContextDependentProblemStepSolver;

public class QuantifierEliminationProblem extends ContextDependentExpressionProblem {
	private static final long serialVersionUID = 1L;
	
	public QuantifierEliminationProblem(Context context) {
		super(context);
	}

	@Override
	public ContextDependentExpressionProblem createSubProblem(ContextDependentProblemStepSolver<Expression> stepSolver, Context localContext) {
		return null; // TODO
	}
	
	@Override
	public ContextDependentProblemStepSolver<Expression> getLocalStepSolver() {
		return null; // TODO
	}
}
