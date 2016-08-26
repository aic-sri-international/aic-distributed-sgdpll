package com.sri.ai.distributed.sgdpllt.dist;

import com.sri.ai.distributed.sgdpllt.wrapper.TheoryWrapper;
import com.sri.ai.expresso.api.Expression;
import com.sri.ai.grinder.sgdpllt.api.Context;
import com.sri.ai.grinder.sgdpllt.api.ContextDependentExpressionProblemStepSolver;
import com.sri.ai.grinder.sgdpllt.api.SingleVariableConstraint;
import com.sri.ai.grinder.sgdpllt.api.Theory;
import com.sri.ai.grinder.sgdpllt.core.solver.QuantifierEliminationStepSolver;
import com.sri.ai.grinder.sgdpllt.group.AssociativeCommutativeGroup;

import akka.actor.ActorRef;
import akka.japi.Creator;

public class TheoryWithDistributedQuantifierEliminatorStepSolvers extends TheoryWrapper {
	private static final long serialVersionUID = 1L;
	
	private transient ActorRef contextDependentExpressionProblemSolverActor;

	public TheoryWithDistributedQuantifierEliminatorStepSolvers(Creator<Theory> theoryCreator, ActorRef contextDependentExpressionProblemSolverActor) throws Exception {
		super(theoryCreator);
		this.contextDependentExpressionProblemSolverActor = contextDependentExpressionProblemSolverActor;
	}
	
	public void setContextDependentExpressionProblemSolverActor(ActorRef contextDependentExpressionProblemSolverActor) {
		this.contextDependentExpressionProblemSolverActor = contextDependentExpressionProblemSolverActor;
	}
	
	@Override
	public ContextDependentExpressionProblemStepSolver getSingleVariableConstraintQuantifierEliminatorStepSolver(AssociativeCommutativeGroup group, SingleVariableConstraint constraint, Expression currentBody, Context context) {
		QuantifierEliminationStepSolver localQuantifierEliminatorStepSolver = (QuantifierEliminationStepSolver) super.getSingleVariableConstraintQuantifierEliminatorStepSolver(group, constraint, currentBody, context);

		QuantifierEliminationStepSolver result = new DistributedQuantifierEliminationStepSolver(localQuantifierEliminatorStepSolver, contextDependentExpressionProblemSolverActor);
		
		return result;
	}
}
