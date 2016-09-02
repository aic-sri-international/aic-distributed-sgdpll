package com.sri.ai.distributed.sgdpllt.wrapper;

import java.util.Random;

import com.sri.ai.expresso.api.Expression;
import com.sri.ai.grinder.sgdpllt.api.SingleVariableConstraint;
import com.sri.ai.grinder.sgdpllt.api.Theory;
import com.sri.ai.grinder.sgdpllt.core.solver.QuantifierEliminationStepSolver;
import com.sri.ai.grinder.sgdpllt.group.AssociativeCommutativeGroup;

import akka.japi.Creator;

public class QuantifierEliminationStepSolverWrapper extends ContextDependentExpressionProblemStepSolverWrapper implements QuantifierEliminationStepSolver {
	private static final long serialVersionUID = 1L;

	public QuantifierEliminationStepSolverWrapper(
			Creator<QuantifierEliminationStepSolver> quantifierEliminationStepSolverCreator) {
		super(quantifierEliminationStepSolverCreator);
	}

	//
	// START - QuantifierEliminationStepSolver
	@Override
	public AssociativeCommutativeGroup getGroup() {
		return getLocalWrappedQuantifierEliminationStepSolver().getGroup();
	}

	@Override
	public SingleVariableConstraint getIndexConstraint() {
		return getLocalWrappedQuantifierEliminationStepSolver().getIndexConstraint();
	}

	@Override
	public Theory getTheory() {
		return getLocalWrappedQuantifierEliminationStepSolver().getTheory();
	}

	@Override
	public Expression getIndex() {
		return getLocalWrappedQuantifierEliminationStepSolver().getIndex();
	}
	
	@Override
	public Expression getBody() {
		return getLocalWrappedQuantifierEliminationStepSolver().getBody();
	}

	@Override
	public Expression makeRandomUnconditionalBody(Random random) {
		return getLocalWrappedQuantifierEliminationStepSolver().makeRandomUnconditionalBody(random);
	}
	// END - QuantifierEliminationStepSolver
	//

	@Override
	public QuantifierEliminationStepSolverWrapper clone() {
		return (QuantifierEliminationStepSolverWrapper) super.clone();
	}

	public QuantifierEliminationStepSolver getLocalWrappedQuantifierEliminationStepSolver() {
		QuantifierEliminationStepSolver result = (QuantifierEliminationStepSolver) getLocalWrappedContextDependentExpressionProblemStepSolver();
		return result;
	}
}