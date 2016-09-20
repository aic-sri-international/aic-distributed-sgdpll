package com.sri.ai.distributed.sgdpllt.wrapper;

import java.io.Serializable;

import com.sri.ai.expresso.api.Expression;
import com.sri.ai.grinder.sgdpllt.api.Context;
import com.sri.ai.grinder.sgdpllt.api.ExpressionStepSolver;

import akka.japi.Creator;

public class ExpressionStepSolverWrapper implements ExpressionStepSolver, Serializable {
	private static final long serialVersionUID = 1L;

	private Creator<? extends ExpressionStepSolver> contextDependentExpressionProblemStepSolverCreator;
	
	// DON'T SERIALIZE
	private transient ExpressionStepSolver wrappedContextDependentExpressionProblemStepSolver;
	 
	public ExpressionStepSolverWrapper(Creator<? extends ExpressionStepSolver> contextDependentExpressionProblemStepSolverCreator) {
		setContextDependentExpressionProblemStepSolverCreator(contextDependentExpressionProblemStepSolverCreator);
	}
	
	public Creator<? extends ExpressionStepSolver> getContextDependentExpressionProblemStepSolverCreator() {
		return contextDependentExpressionProblemStepSolverCreator;
	}
	
	public void setContextDependentExpressionProblemStepSolverCreator(Creator<? extends ExpressionStepSolver> contextDependentExpressionProblemStepSolverCreator) {
		this.contextDependentExpressionProblemStepSolverCreator = contextDependentExpressionProblemStepSolverCreator;
	}
	
	//
	// START - ContextDependentExpressionProblemStepSolver
	@Override
	public Expression solve(Context context) {
		return getLocalWrappedContextDependentExpressionProblemStepSolver().solve(context);
	}

	@Override
	public ExpressionStepSolverWrapper clone() {
		try {
			return (ExpressionStepSolverWrapper) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public SolverStep step(Context context) {
		return getLocalWrappedContextDependentExpressionProblemStepSolver().step(context);
	}
	// END - ContextDependentExpressionProblemStepSolver
	//
	
	public ExpressionStepSolver getLocalWrappedContextDependentExpressionProblemStepSolver() {
		if (wrappedContextDependentExpressionProblemStepSolver == null) {
			try {
				wrappedContextDependentExpressionProblemStepSolver = getContextDependentExpressionProblemStepSolverCreator().create();
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}
		return wrappedContextDependentExpressionProblemStepSolver;
	}
}
