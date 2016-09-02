package com.sri.ai.distributed.sgdpllt.message;

import com.sri.ai.distributed.sgdpllt.dist.DistributedSatisfiabilityOfSingleVariableStepSolver;
import com.sri.ai.expresso.api.Expression;
import com.sri.ai.grinder.sgdpllt.api.Context;
import com.sri.ai.grinder.sgdpllt.api.ContextDependentExpressionProblemStepSolver;
import com.sri.ai.grinder.sgdpllt.api.ContextDependentProblemStepSolver;

import akka.actor.ActorRefFactory;
import akka.event.LoggingAdapter;

public class SatisfiabilityProblem extends ContextDependentExpressionProblem {
	private static final long serialVersionUID = 1L;
	
	private DistributedSatisfiabilityOfSingleVariableStepSolver distributedSatisfiabilityOfSingleVariableStepSolver;
	
	public SatisfiabilityProblem(Context context, DistributedSatisfiabilityOfSingleVariableStepSolver distributedSatisfiabilityOfSingleVariableStepSolver) {
		super(context);
		this.distributedSatisfiabilityOfSingleVariableStepSolver = distributedSatisfiabilityOfSingleVariableStepSolver;
	}
	
	@Override
	public void setLocalActorInfo(ActorRefFactory actorRefFactory, LoggingAdapter actorLog) {
		super.setLocalActorInfo(actorRefFactory, actorLog);
		this.distributedSatisfiabilityOfSingleVariableStepSolver.setLocalActorInfo(actorRefFactory, actorLog);
	}

	@Override
	public ContextDependentExpressionProblem createSubProblem(ContextDependentProblemStepSolver<Expression> localStepSolver, Context localContext) {
		if (localStepSolver instanceof ContextDependentExpressionProblemStepSolver) {
			return new SatisfiabilityProblem(localContext, new DistributedSatisfiabilityOfSingleVariableStepSolver((ContextDependentExpressionProblemStepSolver) localStepSolver));
		}
		else {
			throw new IllegalArgumentException("Unexpected local step solver:"+localStepSolver);
		}
	}
	
	@Override
	public ContextDependentProblemStepSolver<Expression> getLocalStepSolver() {
		// This object knows how to grant access to the local step solver (i.e. it wraps it).
		return distributedSatisfiabilityOfSingleVariableStepSolver.getLocalWrappedContextDependentExpressionProblemStepSolver();
	}
}
