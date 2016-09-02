package com.sri.ai.distributed.sgdpllt.wrapper;

import java.io.Serializable;

import com.sri.ai.expresso.api.Expression;
import com.sri.ai.grinder.sgdpllt.api.Context;
import com.sri.ai.grinder.sgdpllt.api.ContextDependentExpressionProblemStepSolver;

import akka.japi.Creator;

public class ContextDependentExpressionProblemStepSolverWrapper implements ContextDependentExpressionProblemStepSolver, Serializable {
	private static final long serialVersionUID = 1L;

	private Creator<? extends ContextDependentExpressionProblemStepSolver> contextDependentExpressionProblemStepSolverCreator;
	
	// DON'T SERIALIZE
	private transient ContextDependentExpressionProblemStepSolver wrappedContextDependentExpressionProblemStepSolver;
	 
	public ContextDependentExpressionProblemStepSolverWrapper(Creator<? extends ContextDependentExpressionProblemStepSolver> contextDependentExpressionProblemStepSolverCreator) {
		setContextDependentExpressionProblemStepSolverCreator(contextDependentExpressionProblemStepSolverCreator);
	}
	
	public Creator<? extends ContextDependentExpressionProblemStepSolver> getContextDependentExpressionProblemStepSolverCreator() {
		return contextDependentExpressionProblemStepSolverCreator;
	}
	
	public void setContextDependentExpressionProblemStepSolverCreator(Creator<? extends ContextDependentExpressionProblemStepSolver> contextDependentExpressionProblemStepSolverCreator) {
		this.contextDependentExpressionProblemStepSolverCreator = contextDependentExpressionProblemStepSolverCreator;
	}
	
	//
	// START - ContextDependentExpressionProblemStepSolver
	@Override
	public Expression solve(Context context) {
		return getLocalWrappedContextDependentExpressionProblemStepSolver().solve(context);
	}

	@Override
	public ContextDependentExpressionProblemStepSolverWrapper clone() {
		try {
			return (ContextDependentExpressionProblemStepSolverWrapper) super.clone();
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
	
	public ContextDependentExpressionProblemStepSolver getLocalWrappedContextDependentExpressionProblemStepSolver() {
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
