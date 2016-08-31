package com.sri.ai.distributed.sgdpllt.dist;

import java.util.concurrent.TimeUnit;

import com.sri.ai.distributed.sgdpllt.actor.ContextDependentExpressionProblemSolverActor;
import com.sri.ai.distributed.sgdpllt.message.ContextDependentExpressionSolution;
import com.sri.ai.distributed.sgdpllt.message.QuantifierEliminationProblem;
import com.sri.ai.distributed.sgdpllt.wrapper.QuantifierEliminationStepSolverWrapper;
import com.sri.ai.expresso.api.Expression;
import com.sri.ai.grinder.sgdpllt.api.Context;
import com.sri.ai.grinder.sgdpllt.api.SingleVariableConstraint;
import com.sri.ai.grinder.sgdpllt.core.solver.AbstractQuantifierEliminationStepSolver;
import com.sri.ai.grinder.sgdpllt.core.solver.QuantifierEliminationOnBodyInWhichIndexOnlyOccursInsideLiteralsStepSolver;
import com.sri.ai.grinder.sgdpllt.core.solver.QuantifierEliminationStepSolver;
import com.sri.ai.grinder.sgdpllt.theory.differencearithmetic.SummationOnDifferenceArithmeticAndPolynomialStepSolver;
import com.sri.ai.grinder.sgdpllt.theory.linearrealarithmetic.SummationOnLinearRealArithmeticAndPolynomialStepSolver;

import akka.actor.ActorRef;
import akka.actor.ActorRefFactory;
import akka.event.LoggingAdapter;
import akka.japi.Creator;
import akka.pattern.Patterns;
import akka.util.Timeout;
import scala.concurrent.Await;
import scala.concurrent.Future;

public class DistributedQuantifierEliminationStepSolver extends QuantifierEliminationStepSolverWrapper {
	private static final long serialVersionUID = 1L;

	// TODO - make configurable
	private static final Timeout _defaultTimeout = new Timeout(60, TimeUnit.SECONDS);

	private transient ActorRefFactory actorRefFactory;
	private transient LoggingAdapter localLog;

	public DistributedQuantifierEliminationStepSolver(
			QuantifierEliminationStepSolver localQuantifierEliminatorStepSolver) {
		super(constructCreator(localQuantifierEliminatorStepSolver));
	}

	public DistributedQuantifierEliminationStepSolver(
			QuantifierEliminationStepSolver localQuantifierEliminatorStepSolver, ActorRefFactory actorRefFactory,
			LoggingAdapter localLog) {
		super(constructCreator(localQuantifierEliminatorStepSolver));
		setActorRefFactory(actorRefFactory);
		setLocalLog(localLog);

		updateCreator();
	}

	public void setActorRefFactory(ActorRefFactory actorRefFactory) {
		this.actorRefFactory = actorRefFactory;
		//
		updateCreator();
	}

	public void setLocalLog(LoggingAdapter localLog) {
		this.localLog = localLog;
	}

	@Override
	public Expression solve(Context context) {
		return solve(context, "root", this);
	}

	@Override
	public String toString() {
		return "DQEL-local=" + this.getLocalWrappedQuantifierEliminationStepSolver().getClass().getSimpleName();
	}

	protected void updateCreator() {
		// Ensure the appropriate distributed solver is used.
		((CreatorForDistributedQuantifierEliminationStepSolver) this
				.getQuantifierEliminationStepSolverCreator()).distSolver = this;
	}

	public static Expression solve(Context context, String solverType,
			DistributedQuantifierEliminationStepSolver distSolver) {
		Expression result;
		distSolver.localLog.debug("DQEL-solve-{}:{}:idx={}, idx constraint={}, body={}", new Object[] {solverType, distSolver.getLocalWrappedQuantifierEliminationStepSolver(), distSolver.getIndex(),
				distSolver.getIndexConstraint(), distSolver.getBody()});
		QuantifierEliminationProblem quantifierEliminationProblem = new QuantifierEliminationProblem(context,
				distSolver);
		ActorRef contextDependentExpressionProblemSolverActor = distSolver.actorRefFactory
				.actorOf(ContextDependentExpressionProblemSolverActor.props());
		Future<Object> futureResult = Patterns.ask(contextDependentExpressionProblemSolverActor,
				quantifierEliminationProblem, _defaultTimeout);
		try {
			// TODO - ideally, do not want to use blocking but have to for the
			// time being to work with existing aic-expresso control flow.
			ContextDependentExpressionSolution solution = (ContextDependentExpressionSolution) Await
					.result(futureResult, _defaultTimeout.duration());
			result = solution.getLocalValue();

			// Ensure we clean up the actor.
			distSolver.actorRefFactory.stop(contextDependentExpressionProblemSolverActor);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}

		return result;
	}

	public static Creator<QuantifierEliminationStepSolver> constructCreator(
			final QuantifierEliminationStepSolver localQuantifierEliminatorStepSolver) {
		if (localQuantifierEliminatorStepSolver instanceof QuantifierEliminationOnBodyInWhichIndexOnlyOccursInsideLiteralsStepSolver) {
			return new CreatorForQuantifierEliminationOnBodyInWhichIndexOnlyOccursInsideLiteralsStepSolver(
					(QuantifierEliminationOnBodyInWhichIndexOnlyOccursInsideLiteralsStepSolver) localQuantifierEliminatorStepSolver);
		} else if (localQuantifierEliminatorStepSolver instanceof SummationOnDifferenceArithmeticAndPolynomialStepSolver) {
			return new CreatorForSummationOnDifferenceArithmeticAndPolynomialStepSolver(
					(SummationOnDifferenceArithmeticAndPolynomialStepSolver) localQuantifierEliminatorStepSolver);
		} else if (localQuantifierEliminatorStepSolver instanceof SummationOnLinearRealArithmeticAndPolynomialStepSolver) {
			return new CreatorSummationOnLinearRealArithmeticAndPolynomialStepSolver(
					(SummationOnLinearRealArithmeticAndPolynomialStepSolver) localQuantifierEliminatorStepSolver);
		} else {
			throw new IllegalArgumentException("QuantifierEliminationStepSolver:" + localQuantifierEliminatorStepSolver
					+ " type currently not supported.");
		}
	}

	public abstract static class CreatorForDistributedQuantifierEliminationStepSolver
			implements Creator<QuantifierEliminationStepSolver> {
		private static final long serialVersionUID = 1L;

		public transient DistributedQuantifierEliminationStepSolver distSolver;
	}

	public static class CreatorForQuantifierEliminationOnBodyInWhichIndexOnlyOccursInsideLiteralsStepSolver
			extends CreatorForDistributedQuantifierEliminationStepSolver {
		private static final long serialVersionUID = 1L;

		// TODO - need to serialize parts
		private transient QuantifierEliminationOnBodyInWhichIndexOnlyOccursInsideLiteralsStepSolver localSolver;
		
		public CreatorForQuantifierEliminationOnBodyInWhichIndexOnlyOccursInsideLiteralsStepSolver(
				QuantifierEliminationOnBodyInWhichIndexOnlyOccursInsideLiteralsStepSolver localSolver) {
			this.localSolver = localSolver;
		}

		@Override
		public QuantifierEliminationStepSolver create() {
			return new DistQuantifierEliminationOnBodyInWhichIndexOnlyOccursInsideLiteralsStepSolver(this.distSolver, this.localSolver);
		}
	}
	
	public static class DistQuantifierEliminationOnBodyInWhichIndexOnlyOccursInsideLiteralsStepSolver extends QuantifierEliminationOnBodyInWhichIndexOnlyOccursInsideLiteralsStepSolver {
		private transient DistributedQuantifierEliminationStepSolver distSolver;
		public DistQuantifierEliminationOnBodyInWhichIndexOnlyOccursInsideLiteralsStepSolver(DistributedQuantifierEliminationStepSolver distSolver, QuantifierEliminationOnBodyInWhichIndexOnlyOccursInsideLiteralsStepSolver localSolver) {
			super(localSolver.getGroup(), localSolver.getIndexConstraint(), localSolver.getBody());
			this.distSolver = distSolver;
		}
		
		@Override
		public Expression solve(Context context) {
			return DistributedQuantifierEliminationStepSolver.solve(context, "qeobiwiooilss", distSolver);
		}
		
		@Override
		public DistQuantifierEliminationOnBodyInWhichIndexOnlyOccursInsideLiteralsStepSolver clone() {
			DistributedQuantifierEliminationStepSolver cloneDistSolver = new DistributedQuantifierEliminationStepSolver(this, distSolver.actorRefFactory, distSolver.localLog);
			return (DistQuantifierEliminationOnBodyInWhichIndexOnlyOccursInsideLiteralsStepSolver) cloneDistSolver.getLocalWrappedQuantifierEliminationStepSolver();
		}
	}
	
	public static class CreatorForSummationOnDifferenceArithmeticAndPolynomialStepSolver
			extends CreatorForDistributedQuantifierEliminationStepSolver {
		private static final long serialVersionUID = 1L;

		// TODO - need to serialize parts
		private transient SummationOnDifferenceArithmeticAndPolynomialStepSolver localSolver;
		
		public CreatorForSummationOnDifferenceArithmeticAndPolynomialStepSolver(
				SummationOnDifferenceArithmeticAndPolynomialStepSolver localSolver) {
			this.localSolver = localSolver;
		}

		@Override
		public QuantifierEliminationStepSolver create() {
			return new DistSummationOnDifferenceArithmeticAndPolynomialStepSolver(distSolver, localSolver);
		}
	}
	
	public static class DistSummationOnDifferenceArithmeticAndPolynomialStepSolver extends SummationOnDifferenceArithmeticAndPolynomialStepSolver {
		private transient DistributedQuantifierEliminationStepSolver distSolver;
		public DistSummationOnDifferenceArithmeticAndPolynomialStepSolver(DistributedQuantifierEliminationStepSolver distSolver, SummationOnDifferenceArithmeticAndPolynomialStepSolver localSolver) {
			super(localSolver.getIndexConstraint(), localSolver.getBody());
			this.distSolver = distSolver;
		}
		
		@Override
		public Expression solve(Context context) {
			return DistributedQuantifierEliminationStepSolver.solve(context, "sodaapss", distSolver);
		}
		
		@Override
		public DistSummationOnDifferenceArithmeticAndPolynomialStepSolver clone() {
			DistributedQuantifierEliminationStepSolver cloneDistSolver = new DistributedQuantifierEliminationStepSolver(this, distSolver.actorRefFactory, distSolver.localLog);
			return (DistSummationOnDifferenceArithmeticAndPolynomialStepSolver) cloneDistSolver.getLocalWrappedQuantifierEliminationStepSolver();
		}
		
		@Override
		protected AbstractQuantifierEliminationStepSolver makeWithNewIndexConstraint(SingleVariableConstraint newIndexConstraint) {
			DistributedQuantifierEliminationStepSolver newDistSolver = new DistributedQuantifierEliminationStepSolver(super.makeWithNewIndexConstraint(newIndexConstraint), distSolver.actorRefFactory, distSolver.localLog);
			return (DistSummationOnDifferenceArithmeticAndPolynomialStepSolver) newDistSolver.getLocalWrappedQuantifierEliminationStepSolver();
		}
	}

	public static class CreatorSummationOnLinearRealArithmeticAndPolynomialStepSolver
			extends CreatorForDistributedQuantifierEliminationStepSolver {
		private static final long serialVersionUID = 1L;
		
		// TODO - need to serialize parts
		private transient SummationOnLinearRealArithmeticAndPolynomialStepSolver localSolver;

		public CreatorSummationOnLinearRealArithmeticAndPolynomialStepSolver(
				SummationOnLinearRealArithmeticAndPolynomialStepSolver localSolver) {
			this.localSolver = localSolver;
		}

		@Override
		public QuantifierEliminationStepSolver create() {
			return new DistSummationOnLinearRealArithmeticAndPolynomialStepSolver(this.distSolver, localSolver);
		}
	}	
	
	public static class DistSummationOnLinearRealArithmeticAndPolynomialStepSolver extends SummationOnLinearRealArithmeticAndPolynomialStepSolver {
		private transient DistributedQuantifierEliminationStepSolver distSolver;
		public DistSummationOnLinearRealArithmeticAndPolynomialStepSolver(DistributedQuantifierEliminationStepSolver distSolver, SummationOnLinearRealArithmeticAndPolynomialStepSolver localSolver) {
			super(localSolver.getIndexConstraint(), localSolver.getBody());
			this.distSolver = distSolver;
		}
		
		@Override
		public Expression solve(Context context) {
			return DistributedQuantifierEliminationStepSolver.solve(context, "solraapss", distSolver);
		}
		
		@Override
		public DistSummationOnLinearRealArithmeticAndPolynomialStepSolver clone() {
			DistributedQuantifierEliminationStepSolver cloneDistSolver = new DistributedQuantifierEliminationStepSolver(this, distSolver.actorRefFactory, distSolver.localLog);
			return (DistSummationOnLinearRealArithmeticAndPolynomialStepSolver) cloneDistSolver.getLocalWrappedQuantifierEliminationStepSolver();
		}
		
		@Override
		protected AbstractQuantifierEliminationStepSolver makeWithNewIndexConstraint(SingleVariableConstraint newIndexConstraint) {
			DistributedQuantifierEliminationStepSolver newDistSolver = new DistributedQuantifierEliminationStepSolver(super.makeWithNewIndexConstraint(newIndexConstraint), distSolver.actorRefFactory, distSolver.localLog);
			return (DistSummationOnLinearRealArithmeticAndPolynomialStepSolver) newDistSolver.getLocalWrappedQuantifierEliminationStepSolver();
		}
	}
}