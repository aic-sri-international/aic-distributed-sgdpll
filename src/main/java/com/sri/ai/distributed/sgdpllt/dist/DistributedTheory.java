package com.sri.ai.distributed.sgdpllt.dist;

import com.sri.ai.distributed.sgdpllt.wrapper.TheoryWrapper;
import com.sri.ai.expresso.api.Expression;
import com.sri.ai.grinder.sgdpllt.api.Context;
import com.sri.ai.grinder.sgdpllt.api.ContextDependentExpressionProblemStepSolver;
import com.sri.ai.grinder.sgdpllt.api.SingleVariableConstraint;
import com.sri.ai.grinder.sgdpllt.api.Theory;
import com.sri.ai.grinder.sgdpllt.core.solver.QuantifierEliminationStepSolver;
import com.sri.ai.grinder.sgdpllt.group.AssociativeCommutativeGroup;

import akka.actor.ActorRefFactory;
import akka.event.LoggingAdapter;
import akka.japi.Creator;

public class DistributedTheory extends TheoryWrapper {
	private static final long serialVersionUID = 1L;
	
	private transient ActorRefFactory actorRefFactory;
	private transient LoggingAdapter localLog;

	public DistributedTheory(Creator<Theory> theoryCreator, ActorRefFactory actorRefFactory, LoggingAdapter localLog) throws Exception {
		super(theoryCreator);
		setLocalActorInfo(actorRefFactory, localLog);
	}
	
	public void setLocalActorInfo(ActorRefFactory actorRefFactory, LoggingAdapter actorLog) {
		this.actorRefFactory = actorRefFactory;
		this.localLog = actorLog;
	}
	
	@Override
	public ContextDependentExpressionProblemStepSolver getSingleVariableConstraintQuantifierEliminatorStepSolver(AssociativeCommutativeGroup group, SingleVariableConstraint constraint, Expression currentBody, Context context) {
		QuantifierEliminationStepSolver localQuantifierEliminatorStepSolver = (QuantifierEliminationStepSolver) super.getSingleVariableConstraintQuantifierEliminatorStepSolver(group, constraint, currentBody, context);

		DistributedQuantifierEliminationStepSolver result = new DistributedQuantifierEliminationStepSolver(localQuantifierEliminatorStepSolver, actorRefFactory, localLog);
		
		return result.getLocalWrappedQuantifierEliminationStepSolver();
	}
}
