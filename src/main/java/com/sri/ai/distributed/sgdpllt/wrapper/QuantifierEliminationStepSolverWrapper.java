package com.sri.ai.distributed.sgdpllt.wrapper;

import java.io.Serializable;
import java.util.Random;

import com.sri.ai.expresso.api.Expression;
import com.sri.ai.grinder.sgdpllt.api.Context;
import com.sri.ai.grinder.sgdpllt.api.SingleVariableConstraint;
import com.sri.ai.grinder.sgdpllt.api.Theory;
import com.sri.ai.grinder.sgdpllt.core.solver.QuantifierEliminationStepSolver;
import com.sri.ai.grinder.sgdpllt.group.AssociativeCommutativeGroup;
import akka.japi.Creator;

public class QuantifierEliminationStepSolverWrapper implements QuantifierEliminationStepSolver, Serializable {
	private static final long serialVersionUID = 1L;

	private Creator<QuantifierEliminationStepSolver> quantifierEliminationStepSolverCreator;

	// DON'T SERIALIZE
	private transient QuantifierEliminationStepSolver wrappedQuantifierEliminationStepSolver;

	public QuantifierEliminationStepSolverWrapper(
			Creator<QuantifierEliminationStepSolver> quantifierEliminationStepSolverCreator) {
		setQuantifierEliminationStepSolverCreator(quantifierEliminationStepSolverCreator);
	}
	
	public Creator<QuantifierEliminationStepSolver> getQuantifierEliminationStepSolverCreator() {
		return quantifierEliminationStepSolverCreator;
	}
	
	public void setQuantifierEliminationStepSolverCreator(Creator<QuantifierEliminationStepSolver> quantifierEliminationStepSolverCreator) {
		this.quantifierEliminationStepSolverCreator = quantifierEliminationStepSolverCreator;
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

	@Override
	public Expression solve(Context context) {
		return getLocalWrappedQuantifierEliminationStepSolver().solve(context);
	}

	@Override
	public QuantifierEliminationStepSolverWrapper clone() {
		try {
			return (QuantifierEliminationStepSolverWrapper) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public SolverStep step(Context context) {
		return getLocalWrappedQuantifierEliminationStepSolver().step(context);
	}

	// END - QuantifierEliminationStepSolver
	//

	public QuantifierEliminationStepSolver getLocalWrappedQuantifierEliminationStepSolver() {
		if (wrappedQuantifierEliminationStepSolver == null) {
			try {
				wrappedQuantifierEliminationStepSolver = getQuantifierEliminationStepSolverCreator().create();
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}
		return wrappedQuantifierEliminationStepSolver;
	}
}