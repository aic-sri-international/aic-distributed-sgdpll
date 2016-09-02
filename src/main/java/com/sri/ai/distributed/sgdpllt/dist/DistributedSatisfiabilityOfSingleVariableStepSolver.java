package com.sri.ai.distributed.sgdpllt.dist;

import java.util.concurrent.TimeUnit;

import com.sri.ai.distributed.sgdpllt.actor.ContextDependentExpressionProblemSolverActor;
import com.sri.ai.distributed.sgdpllt.message.ContextDependentExpressionSolution;
import com.sri.ai.distributed.sgdpllt.message.SatisfiabilityProblem;
import com.sri.ai.distributed.sgdpllt.util.TestSerialize;
import com.sri.ai.distributed.sgdpllt.wrapper.ContextDependentExpressionProblemStepSolverWrapper;
import com.sri.ai.expresso.api.Expression;
import com.sri.ai.grinder.sgdpllt.api.Context;
import com.sri.ai.grinder.sgdpllt.api.ContextDependentExpressionProblemStepSolver;
import com.sri.ai.grinder.sgdpllt.api.SingleVariableConstraint;
import com.sri.ai.grinder.sgdpllt.theory.differencearithmetic.SatisfiabilityOfSingleVariableDifferenceArithmeticConstraintStepSolver;
import com.sri.ai.grinder.sgdpllt.theory.differencearithmetic.SingleVariableDifferenceArithmeticConstraint;
import com.sri.ai.grinder.sgdpllt.theory.equality.SatisfiabilityOfSingleVariableEqualityConstraintStepSolver;
import com.sri.ai.grinder.sgdpllt.theory.equality.SingleVariableEqualityConstraint;
import com.sri.ai.grinder.sgdpllt.theory.linearrealarithmetic.SatisfiabilityOfSingleVariableLinearRealArithmeticConstraintStepSolver;
import com.sri.ai.grinder.sgdpllt.theory.linearrealarithmetic.SingleVariableLinearRealArithmeticConstraint;
import com.sri.ai.grinder.sgdpllt.theory.propositional.SatisfiabilityOfSingleVariablePropositionalConstraintStepSolver;
import com.sri.ai.grinder.sgdpllt.theory.propositional.SingleVariablePropositionalConstraint;

import akka.actor.ActorRef;
import akka.actor.ActorRefFactory;
import akka.event.LoggingAdapter;
import akka.japi.Creator;
import akka.pattern.Patterns;
import akka.util.Timeout;
import scala.concurrent.Await;
import scala.concurrent.Future;

public class DistributedSatisfiabilityOfSingleVariableStepSolver
		extends ContextDependentExpressionProblemStepSolverWrapper {
	private static final long serialVersionUID = 1L;

	// TODO - make configurable
	private static final Timeout _defaultTimeout = new Timeout(3600, TimeUnit.SECONDS);

	private transient ActorRefFactory actorRefFactory;
	private transient LoggingAdapter localLog;

	public DistributedSatisfiabilityOfSingleVariableStepSolver(
			ContextDependentExpressionProblemStepSolver localSatisfiabilityStepSolver) {
		super(constructCreator(localSatisfiabilityStepSolver));
	}

	public DistributedSatisfiabilityOfSingleVariableStepSolver(
			ContextDependentExpressionProblemStepSolver localSatisfiabilityStepSolver, ActorRefFactory actorRefFactory,
			LoggingAdapter localLog) {
		super(constructCreator(localSatisfiabilityStepSolver));
		this.actorRefFactory = actorRefFactory;
		this.localLog = localLog;

		updateCreator();
	}

	public void setLocalActorInfo(ActorRefFactory actorRefFactory, LoggingAdapter actorLog) {
		this.actorRefFactory = actorRefFactory;
		this.localLog = actorLog;
		//
		updateCreator();
	}

	@Override
	public Expression solve(Context context) {
		return solve(context, "root", this);
	}

	@Override
	public String toString() {
		return "DSAT-local="
				+ this.getLocalWrappedContextDependentExpressionProblemStepSolver().getClass().getSimpleName();
	}

	protected void updateCreator() {
		// Ensure the appropriate distributed solver is used.
		((CreatorForDistributedSatisfiabilityOfSingleVariableStepSolver) this
				.getContextDependentExpressionProblemStepSolverCreator()).distSolver = this;
	}

	public static Expression solve(Context context, String solverType,
			DistributedSatisfiabilityOfSingleVariableStepSolver distSolver) {
		Expression result;
		distSolver.localLog.debug("DSAT-solve-{}:{}:{}",
				new Object[] { solverType, distSolver.getLocalWrappedContextDependentExpressionProblemStepSolver(),
						((ContainsSingleVariableConstraint)distSolver.getLocalWrappedContextDependentExpressionProblemStepSolver()).getConstraint()});
		SatisfiabilityProblem satisfiabilityProblem = new SatisfiabilityProblem(context, distSolver);
		ActorRef contextDependentExpressionProblemSolverActor = distSolver.actorRefFactory
				.actorOf(ContextDependentExpressionProblemSolverActor.props());
		Future<Object> futureResult = Patterns.ask(contextDependentExpressionProblemSolverActor,
				TestSerialize.serializeMessage(satisfiabilityProblem, distSolver.localLog), _defaultTimeout);
		try {
			// TODO - ideally, do not want to use blocking but have to for the
			// time being to work with existing aic-expresso control flow.
			ContextDependentExpressionSolution solution = (ContextDependentExpressionSolution) Await
					.result(futureResult, _defaultTimeout.duration());
			result = solution.getLocalValue();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}

		return result;
	}

	public static Creator<ContextDependentExpressionProblemStepSolver> constructCreator(
			final ContextDependentExpressionProblemStepSolver localSatisfiabilityStepSolver) {

		if (localSatisfiabilityStepSolver instanceof SatisfiabilityOfSingleVariableDifferenceArithmeticConstraintStepSolver) {
			return new CreatorForSatisfiabilityOfSingleVariableDifferenceArithmeticConstraintStepSolver(
					(SatisfiabilityOfSingleVariableDifferenceArithmeticConstraintStepSolver) localSatisfiabilityStepSolver);
		} else if (localSatisfiabilityStepSolver instanceof SatisfiabilityOfSingleVariableEqualityConstraintStepSolver) {
			return new CreatorForSatisfiabilityOfSingleVariableEqualityConstraintStepSolver(
					(SatisfiabilityOfSingleVariableEqualityConstraintStepSolver) localSatisfiabilityStepSolver);
		} else if (localSatisfiabilityStepSolver instanceof SatisfiabilityOfSingleVariableLinearRealArithmeticConstraintStepSolver) {
			return new CreatorForSatisfiabilityOfSingleVariableLinearRealArithmeticConstraintStepSolver(
					(SatisfiabilityOfSingleVariableLinearRealArithmeticConstraintStepSolver) localSatisfiabilityStepSolver);
		} else if (localSatisfiabilityStepSolver instanceof SatisfiabilityOfSingleVariablePropositionalConstraintStepSolver) {
			return new CreatorForSatisfiabilityOfSingleVariablePropositionalConstraintStepSolver(
					(SatisfiabilityOfSingleVariablePropositionalConstraintStepSolver) localSatisfiabilityStepSolver);
		} else {
			throw new IllegalArgumentException("ContextDependentExpressionProblemStepSolver:"
					+ localSatisfiabilityStepSolver + " type currently not supported.");
		}
	}

	public abstract static class CreatorForDistributedSatisfiabilityOfSingleVariableStepSolver
			implements Creator<ContextDependentExpressionProblemStepSolver> {
		private static final long serialVersionUID = 1L;

		public transient DistributedSatisfiabilityOfSingleVariableStepSolver distSolver;
		
		protected SingleVariableConstraint constraint;

		public CreatorForDistributedSatisfiabilityOfSingleVariableStepSolver(SingleVariableConstraint constraint) {
			this.constraint = constraint;
		}
	}
	
	public interface ContainsSingleVariableConstraint {
		SingleVariableConstraint getConstraint();
	}
	
	// SatisfiabilityOfSingleVariableDifferenceArithmeticConstraintStepSolver
	public static class CreatorForSatisfiabilityOfSingleVariableDifferenceArithmeticConstraintStepSolver extends CreatorForDistributedSatisfiabilityOfSingleVariableStepSolver {
		private static final long serialVersionUID = 1L;
		
		public CreatorForSatisfiabilityOfSingleVariableDifferenceArithmeticConstraintStepSolver(SatisfiabilityOfSingleVariableDifferenceArithmeticConstraintStepSolver localSolver) {
			super(localSolver.getConstraint());
		}
		
		@Override
		public ContextDependentExpressionProblemStepSolver create() throws Exception {
			return new DistSatisfiabilityOfSingleVariableDifferenceArithmeticConstraintStepSolver(this.distSolver, constraint);
		}
	}
	
	public static class DistSatisfiabilityOfSingleVariableDifferenceArithmeticConstraintStepSolver extends SatisfiabilityOfSingleVariableDifferenceArithmeticConstraintStepSolver implements ContainsSingleVariableConstraint {
		private transient DistributedSatisfiabilityOfSingleVariableStepSolver distSolver;
		
		public DistSatisfiabilityOfSingleVariableDifferenceArithmeticConstraintStepSolver(DistributedSatisfiabilityOfSingleVariableStepSolver distSolver, SatisfiabilityOfSingleVariableDifferenceArithmeticConstraintStepSolver localSolver) {
			this(distSolver, localSolver.getConstraint());
		}
		
		public DistSatisfiabilityOfSingleVariableDifferenceArithmeticConstraintStepSolver(DistributedSatisfiabilityOfSingleVariableStepSolver distSolver, SingleVariableConstraint constraint) {
			super((SingleVariableDifferenceArithmeticConstraint)constraint);
			this.distSolver = distSolver;
		}
		
		@Override
		public Expression solve(Context context) {
			return DistributedSatisfiabilityOfSingleVariableStepSolver.solve(context, "satdiffarr", distSolver);
		}
		
		@Override
		public DistSatisfiabilityOfSingleVariableDifferenceArithmeticConstraintStepSolver clone() {
			DistributedSatisfiabilityOfSingleVariableStepSolver cloneDistSolver = new DistributedSatisfiabilityOfSingleVariableStepSolver(this, distSolver.actorRefFactory, distSolver.localLog);
			return (DistSatisfiabilityOfSingleVariableDifferenceArithmeticConstraintStepSolver) cloneDistSolver.getLocalWrappedContextDependentExpressionProblemStepSolver();
		}
	}
	
	// SatisfiabilityOfSingleVariableEqualityConstraintStepSolver
	public static class CreatorForSatisfiabilityOfSingleVariableEqualityConstraintStepSolver extends CreatorForDistributedSatisfiabilityOfSingleVariableStepSolver {
		private static final long serialVersionUID = 1L;
		
		public CreatorForSatisfiabilityOfSingleVariableEqualityConstraintStepSolver(SatisfiabilityOfSingleVariableEqualityConstraintStepSolver localSolver) {
			super(localSolver.getConstraint());
		}
		
		@Override
		public ContextDependentExpressionProblemStepSolver create() throws Exception {
			return new DistSatisfiabilityOfSingleVariableEqualityConstraintStepSolver(this.distSolver, constraint);
		}
	}
	
	public static class DistSatisfiabilityOfSingleVariableEqualityConstraintStepSolver extends SatisfiabilityOfSingleVariableEqualityConstraintStepSolver implements ContainsSingleVariableConstraint {
		private transient DistributedSatisfiabilityOfSingleVariableStepSolver distSolver;
		
		public DistSatisfiabilityOfSingleVariableEqualityConstraintStepSolver(DistributedSatisfiabilityOfSingleVariableStepSolver distSolver, SatisfiabilityOfSingleVariableEqualityConstraintStepSolver localSolver) {
			this(distSolver, localSolver.getConstraint());
		}
		
		public DistSatisfiabilityOfSingleVariableEqualityConstraintStepSolver(DistributedSatisfiabilityOfSingleVariableStepSolver distSolver, SingleVariableConstraint constraint) {
			super((SingleVariableEqualityConstraint)constraint);
			this.distSolver = distSolver;
		}
		
		@Override
		public Expression solve(Context context) {
			return DistributedSatisfiabilityOfSingleVariableStepSolver.solve(context, "satequality", distSolver);
		}
		
		@Override
		public DistSatisfiabilityOfSingleVariableEqualityConstraintStepSolver clone() {
			DistributedSatisfiabilityOfSingleVariableStepSolver cloneDistSolver = new DistributedSatisfiabilityOfSingleVariableStepSolver(this, distSolver.actorRefFactory, distSolver.localLog);
			return (DistSatisfiabilityOfSingleVariableEqualityConstraintStepSolver) cloneDistSolver.getLocalWrappedContextDependentExpressionProblemStepSolver();
		}
	}
	
	// SatisfiabilityOfSingleVariableLinearRealArithmeticConstraintStepSolver
	public static class CreatorForSatisfiabilityOfSingleVariableLinearRealArithmeticConstraintStepSolver extends CreatorForDistributedSatisfiabilityOfSingleVariableStepSolver {
		private static final long serialVersionUID = 1L;
		
		public CreatorForSatisfiabilityOfSingleVariableLinearRealArithmeticConstraintStepSolver(SatisfiabilityOfSingleVariableLinearRealArithmeticConstraintStepSolver localSolver) {
			super(localSolver.getConstraint());
		}
		
		@Override
		public ContextDependentExpressionProblemStepSolver create() throws Exception {
			return new DistSatisfiabilityOfSingleVariableLinearRealArithmeticConstraintStepSolver(this.distSolver, constraint);
		}
	}
	
	public static class DistSatisfiabilityOfSingleVariableLinearRealArithmeticConstraintStepSolver extends SatisfiabilityOfSingleVariableLinearRealArithmeticConstraintStepSolver implements ContainsSingleVariableConstraint {
		private transient DistributedSatisfiabilityOfSingleVariableStepSolver distSolver;
		
		public DistSatisfiabilityOfSingleVariableLinearRealArithmeticConstraintStepSolver(DistributedSatisfiabilityOfSingleVariableStepSolver distSolver, SatisfiabilityOfSingleVariableLinearRealArithmeticConstraintStepSolver localSolver) {
			this(distSolver, localSolver.getConstraint());
		}
		
		public DistSatisfiabilityOfSingleVariableLinearRealArithmeticConstraintStepSolver(DistributedSatisfiabilityOfSingleVariableStepSolver distSolver, SingleVariableConstraint constraint) {
			super((SingleVariableLinearRealArithmeticConstraint)constraint);
			this.distSolver = distSolver;
		}
		
		@Override
		public Expression solve(Context context) {
			return DistributedSatisfiabilityOfSingleVariableStepSolver.solve(context, "satlra", distSolver);
		}
		
		@Override
		public DistSatisfiabilityOfSingleVariableLinearRealArithmeticConstraintStepSolver clone() {
			DistributedSatisfiabilityOfSingleVariableStepSolver cloneDistSolver = new DistributedSatisfiabilityOfSingleVariableStepSolver(this, distSolver.actorRefFactory, distSolver.localLog);
			return (DistSatisfiabilityOfSingleVariableLinearRealArithmeticConstraintStepSolver) cloneDistSolver.getLocalWrappedContextDependentExpressionProblemStepSolver();
		}
	}
	
	// SatisfiabilityOfSingleVariablePropositionalConstraintStepSolver
	public static class CreatorForSatisfiabilityOfSingleVariablePropositionalConstraintStepSolver extends CreatorForDistributedSatisfiabilityOfSingleVariableStepSolver {
		private static final long serialVersionUID = 1L;
		
		public CreatorForSatisfiabilityOfSingleVariablePropositionalConstraintStepSolver(SatisfiabilityOfSingleVariablePropositionalConstraintStepSolver localSolver) {
			super(localSolver.getConstraint());
		}
		
		@Override
		public ContextDependentExpressionProblemStepSolver create() throws Exception {
			return new DistSatisfiabilityOfSingleVariablePropositionalConstraintStepSolver(this.distSolver, constraint);
		}
	}
	
	public static class DistSatisfiabilityOfSingleVariablePropositionalConstraintStepSolver extends SatisfiabilityOfSingleVariablePropositionalConstraintStepSolver implements ContainsSingleVariableConstraint {
		private transient DistributedSatisfiabilityOfSingleVariableStepSolver distSolver;
		
		public DistSatisfiabilityOfSingleVariablePropositionalConstraintStepSolver(DistributedSatisfiabilityOfSingleVariableStepSolver distSolver, SatisfiabilityOfSingleVariablePropositionalConstraintStepSolver localSolver) {
			this(distSolver, localSolver.getConstraint());
		}
		
		public DistSatisfiabilityOfSingleVariablePropositionalConstraintStepSolver(DistributedSatisfiabilityOfSingleVariableStepSolver distSolver, SingleVariableConstraint constraint) {
			super((SingleVariablePropositionalConstraint)constraint);
			this.distSolver = distSolver;
		}
		
		@Override
		public Expression solve(Context context) {
			return DistributedSatisfiabilityOfSingleVariableStepSolver.solve(context, "satprop", distSolver);
		}
		
		@Override
		public DistSatisfiabilityOfSingleVariablePropositionalConstraintStepSolver clone() {
			DistributedSatisfiabilityOfSingleVariableStepSolver cloneDistSolver = new DistributedSatisfiabilityOfSingleVariableStepSolver(this, distSolver.actorRefFactory, distSolver.localLog);
			return (DistSatisfiabilityOfSingleVariablePropositionalConstraintStepSolver) cloneDistSolver.getLocalWrappedContextDependentExpressionProblemStepSolver();
		}
	}
}
