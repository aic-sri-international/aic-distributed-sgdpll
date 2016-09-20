package com.sri.ai.distributed.sgdpllt.dist;

import com.sri.ai.distributed.sgdpllt.wrapper.TheoryWrapper;
import com.sri.ai.expresso.api.Expression;
import com.sri.ai.grinder.sgdpllt.api.Context;
import com.sri.ai.grinder.sgdpllt.api.ExpressionStepSolver;
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
	
	// NOTE: The distribution overhead is too high for this step solver (observed a factor of 40 slow down and little parallelization actually occurring).
	private boolean distributeSatisfiabilityStepSolvers = false;
	@Override
	public ExpressionStepSolver getSingleVariableConstraintSatisfiabilityStepSolver(SingleVariableConstraint constraint, Context context) {
		if (!distributeSatisfiabilityStepSolvers) {
			return super.getSingleVariableConstraintSatisfiabilityStepSolver(constraint, context);
		}
		ExpressionStepSolver localStepSolver = super.getSingleVariableConstraintSatisfiabilityStepSolver(constraint, context);
		
		DistributedSatisfiabilityOfSingleVariableStepSolver result = new DistributedSatisfiabilityOfSingleVariableStepSolver(localStepSolver, actorRefFactory, localLog);
		
		return result.getLocalWrappedContextDependentExpressionProblemStepSolver();
	}
	
	@Override
	public ExpressionStepSolver getSingleVariableConstraintQuantifierEliminatorStepSolver(AssociativeCommutativeGroup group, SingleVariableConstraint constraint, Expression currentBody, Context context) {
		QuantifierEliminationStepSolver localQuantifierEliminatorStepSolver = (QuantifierEliminationStepSolver) super.getSingleVariableConstraintQuantifierEliminatorStepSolver(group, constraint, currentBody, context);

		DistributedQuantifierEliminationStepSolver result = new DistributedQuantifierEliminationStepSolver(localQuantifierEliminatorStepSolver, actorRefFactory, localLog);
		
		return result.getLocalWrappedQuantifierEliminationStepSolver();
	}
}
