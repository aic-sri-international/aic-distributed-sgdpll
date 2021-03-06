package com.sri.ai.distributed.sgdpllt.message;

import com.sri.ai.distributed.sgdpllt.dist.DistributedQuantifierEliminationStepSolver;
import com.sri.ai.expresso.api.Expression;
import com.sri.ai.grinder.sgdpllt.api.Context;
import com.sri.ai.grinder.sgdpllt.api.StepSolver;
import com.sri.ai.grinder.sgdpllt.core.solver.QuantifierEliminationStepSolver;

import akka.actor.ActorRefFactory;
import akka.event.LoggingAdapter;

public class QuantifierEliminationProblem extends ContextDependentExpressionProblem {
	private static final long serialVersionUID = 1L;
	
	private DistributedQuantifierEliminationStepSolver distributedQuantifierEliminationStepSolver;
	
	public QuantifierEliminationProblem(Context context, DistributedQuantifierEliminationStepSolver distributedQuantifierEliminationStepSolver) {
		super(context);
		this.distributedQuantifierEliminationStepSolver = distributedQuantifierEliminationStepSolver;
	}
	
	@Override
	public void setLocalActorInfo(ActorRefFactory actorRefFactory, LoggingAdapter actorLog) {
		super.setLocalActorInfo(actorRefFactory, actorLog);
		this.distributedQuantifierEliminationStepSolver.setLocalActorInfo(actorRefFactory, actorLog);
	}

	@Override
	public ContextDependentExpressionProblem createSubProblem(StepSolver<Expression> localStepSolver, Context localContext) {
		if (localStepSolver instanceof QuantifierEliminationStepSolver) {
			return new QuantifierEliminationProblem(localContext, new DistributedQuantifierEliminationStepSolver((QuantifierEliminationStepSolver) localStepSolver));
		}
		else {
			throw new IllegalArgumentException("Unexpected local step solver:"+localStepSolver);
		}
	}
	
	@Override
	public StepSolver<Expression> getLocalStepSolver() {
		// This object knows how to grant access to the local step solver (i.e. it wraps it).
		return distributedQuantifierEliminationStepSolver.getLocalWrappedQuantifierEliminationStepSolver();
	}
}
