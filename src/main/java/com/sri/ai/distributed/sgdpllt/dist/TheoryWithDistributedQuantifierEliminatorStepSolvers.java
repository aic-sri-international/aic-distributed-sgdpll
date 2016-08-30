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

public class TheoryWithDistributedQuantifierEliminatorStepSolvers extends TheoryWrapper {
	private static final long serialVersionUID = 1L;
	
	private transient ActorRefFactory actorRefFactory;
	private transient LoggingAdapter localLog;

	public TheoryWithDistributedQuantifierEliminatorStepSolvers(Creator<Theory> theoryCreator, ActorRefFactory actorRefFactory, LoggingAdapter localLog) throws Exception {
		super(theoryCreator);
		this.actorRefFactory = actorRefFactory;
		this.localLog = localLog;
	}
	
	// NOTE: This logic works under the assumption this method is only called at the top level (i.e. not nested) as it is dependent
	// root 'contextDependentExpressionProblemSolverActor' to kick things off. If this assumption does not hold (i.e. referenced via nested calls)
	// then things will not work as the logic would keep looping back to the root instance.
	@Override
	public ContextDependentExpressionProblemStepSolver getSingleVariableConstraintQuantifierEliminatorStepSolver(AssociativeCommutativeGroup group, SingleVariableConstraint constraint, Expression currentBody, Context context) {
		QuantifierEliminationStepSolver localQuantifierEliminatorStepSolver = (QuantifierEliminationStepSolver) super.getSingleVariableConstraintQuantifierEliminatorStepSolver(group, constraint, currentBody, context);

		DistributedQuantifierEliminationStepSolver result = new DistributedQuantifierEliminationStepSolver(localQuantifierEliminatorStepSolver, actorRefFactory, localLog);
		
		return result.getLocalWrappedQuantifierEliminationStepSolver();
	}
}
