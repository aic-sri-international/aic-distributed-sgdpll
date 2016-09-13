package com.sri.ai.distributed.sgdpllt.dist;

import java.util.concurrent.Callable;

import com.sri.ai.distributed.sgdpllt.actor.ContextDependentExpressionProblemSolverActor;
import com.sri.ai.distributed.sgdpllt.message.ContextDependentExpressionSolution;
import com.sri.ai.distributed.sgdpllt.message.QuantifierEliminationProblem;
import com.sri.ai.distributed.sgdpllt.util.AkkaUtil;
import com.sri.ai.distributed.sgdpllt.util.TestSerialize;
import com.sri.ai.distributed.sgdpllt.wrapper.QuantifierEliminationStepSolverWrapper;
import com.sri.ai.expresso.api.Expression;
import com.sri.ai.grinder.sgdpllt.api.Context;
import com.sri.ai.grinder.sgdpllt.api.SingleVariableConstraint;
import com.sri.ai.grinder.sgdpllt.core.solver.AbstractQuantifierEliminationStepSolver;
import com.sri.ai.grinder.sgdpllt.core.solver.QuantifierEliminationOnBodyInWhichIndexOnlyOccursInsideLiteralsStepSolver;
import com.sri.ai.grinder.sgdpllt.core.solver.QuantifierEliminationStepSolver;
import com.sri.ai.grinder.sgdpllt.group.AssociativeCommutativeGroup;
import com.sri.ai.grinder.sgdpllt.group.Max;
import com.sri.ai.grinder.sgdpllt.group.Product;
import com.sri.ai.grinder.sgdpllt.group.Sum;
import com.sri.ai.grinder.sgdpllt.group.SumProduct;
import com.sri.ai.grinder.sgdpllt.theory.differencearithmetic.SummationOnDifferenceArithmeticAndPolynomialStepSolver;
import com.sri.ai.grinder.sgdpllt.theory.linearrealarithmetic.SummationOnLinearRealArithmeticAndPolynomialStepSolver;
import com.sri.ai.util.base.Pair;

import akka.actor.ActorRef;
import akka.actor.ActorRefFactory;
import akka.dispatch.Futures;
import akka.event.LoggingAdapter;
import akka.japi.Creator;
import akka.pattern.Patterns;
import scala.concurrent.Await;
import scala.concurrent.Future;

public class DistributedQuantifierEliminationStepSolver extends QuantifierEliminationStepSolverWrapper {
	private static final long serialVersionUID = 1L;

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
		this.actorRefFactory = actorRefFactory;
		this.localLog = localLog;

		updateCreator();
	}
	
	public void setLocalActorInfo(ActorRefFactory actorRefFactory, LoggingAdapter actorLog) {
		this.actorRefFactory = actorRefFactory;
		this.localLog = actorLog;
		
		((DistributedTheory)getTheory()).setLocalActorInfo(actorRefFactory, actorLog);
		
		//
		updateCreator();
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
				.getContextDependentExpressionProblemStepSolverCreator()).distSolver = this;
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
				TestSerialize.serializeMessage(quantifierEliminationProblem, distSolver.localLog), AkkaUtil.getDefaultTimeout());
		try {
			// TODO - ideally, do not want to use blocking but have to for the
			// time being to work with existing aic-expresso control flow.
			ContextDependentExpressionSolution solution = (ContextDependentExpressionSolution) Await
					.result(futureResult, AkkaUtil.getDefaultTimeout().duration());
			result = solution.getLocalValue();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}

		return result;
	}
	
	public static Pair<Expression, Expression> solveSubProblems(final AbstractQuantifierEliminationStepSolver subProblem1, final AbstractQuantifierEliminationStepSolver subProblem2, final Context context, DistributedQuantifierEliminationStepSolver distSolver) {
		Future<Expression> subSolution1Future = Futures.future(new Callable<Expression>() {
			public Expression call() {
				return subProblem1.solve(context);
			}
		}, distSolver.actorRefFactory.dispatcher());
		Future<Expression> subSolution2Future = Futures.future(new Callable<Expression>() {
			public Expression call() {
				return subProblem2.solve(context);
			}
		}, distSolver.actorRefFactory.dispatcher());
		
		Pair<Expression, Expression> result;
		try {
			Expression subSolution1 = Await.result(subSolution1Future,  AkkaUtil.getDefaultTimeout().duration());
			Expression subSolution2 = Await.result(subSolution2Future,  AkkaUtil.getDefaultTimeout().duration());
			
			result = new Pair<>(subSolution1, subSolution2);
		}
		catch (Exception ex) {
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
		
		
		protected Creator<AssociativeCommutativeGroup> groupCreator;
		// TODO should be properly serializable versions
		protected SingleVariableConstraint indexConstraint;
		protected Expression body;
		
		public CreatorForDistributedQuantifierEliminationStepSolver(AssociativeCommutativeGroup group, SingleVariableConstraint indexConstraint, Expression body) {
			if (group instanceof SumProduct) {
				groupCreator = new Creator<AssociativeCommutativeGroup>() {	
					private static final long serialVersionUID = 1L;
					@Override
					public AssociativeCommutativeGroup create() {
						return new SumProduct();
					}
				};
			}
			else if (group instanceof Sum) {
				groupCreator = new Creator<AssociativeCommutativeGroup>() {	
					private static final long serialVersionUID = 1L;
					@Override
					public AssociativeCommutativeGroup create() {
						return new Sum();
					}
				};				
			}
			else if (group instanceof Product) {
				groupCreator = new Creator<AssociativeCommutativeGroup>() {	
					private static final long serialVersionUID = 1L;
					@Override
					public AssociativeCommutativeGroup create() {
						return new Product();
					}
				};
			}
			else if (group instanceof Max) {
				groupCreator = new Creator<AssociativeCommutativeGroup>() {	
					private static final long serialVersionUID = 1L;
					@Override
					public AssociativeCommutativeGroup create() {
						return new Max();
					}
				};
			}
			else {
				throw new IllegalArgumentException("AssociativeCommutativeGroup of this type is not supported:"+group);
			}
			this.indexConstraint = indexConstraint.clone();
			this.body = body;
		}
	}

	public static class CreatorForQuantifierEliminationOnBodyInWhichIndexOnlyOccursInsideLiteralsStepSolver
			extends CreatorForDistributedQuantifierEliminationStepSolver {
		private static final long serialVersionUID = 1L;
		private transient DistQuantifierEliminationOnBodyInWhichIndexOnlyOccursInsideLiteralsStepSolver localSolver;
		
		public CreatorForQuantifierEliminationOnBodyInWhichIndexOnlyOccursInsideLiteralsStepSolver(
				QuantifierEliminationOnBodyInWhichIndexOnlyOccursInsideLiteralsStepSolver localSolver) {
			super(localSolver.getGroup(), localSolver.getIndexConstraint(), localSolver.getBody());
			if (localSolver instanceof DistQuantifierEliminationOnBodyInWhichIndexOnlyOccursInsideLiteralsStepSolver) {
				this.localSolver = (DistQuantifierEliminationOnBodyInWhichIndexOnlyOccursInsideLiteralsStepSolver) localSolver;
			}
		}

		@Override
		public QuantifierEliminationStepSolver create() throws Exception {
			QuantifierEliminationStepSolver result;
			if (this.localSolver != null) {	
				result = this.localSolver;
				this.localSolver = null;
			}
			else {				
				result = new DistQuantifierEliminationOnBodyInWhichIndexOnlyOccursInsideLiteralsStepSolver(this.distSolver, groupCreator.create(), indexConstraint, body);
			}
			return result;
		}
	}
	
	public static class DistQuantifierEliminationOnBodyInWhichIndexOnlyOccursInsideLiteralsStepSolver extends QuantifierEliminationOnBodyInWhichIndexOnlyOccursInsideLiteralsStepSolver {
		private transient DistributedQuantifierEliminationStepSolver distSolver;
		public DistQuantifierEliminationOnBodyInWhichIndexOnlyOccursInsideLiteralsStepSolver(DistributedQuantifierEliminationStepSolver distSolver, QuantifierEliminationOnBodyInWhichIndexOnlyOccursInsideLiteralsStepSolver localSolver) {
			this(distSolver, localSolver.getGroup(), localSolver.getIndexConstraint(), localSolver.getBody());			
		}
		
		public DistQuantifierEliminationOnBodyInWhichIndexOnlyOccursInsideLiteralsStepSolver(DistributedQuantifierEliminationStepSolver distSolver, AssociativeCommutativeGroup group, SingleVariableConstraint indexConstraint, Expression body) {
			super(group, indexConstraint, body);
			this.distSolver = distSolver;
		}
		
		@Override
		public Expression solve(Context context) {
			return DistributedQuantifierEliminationStepSolver.solve(context, "qeobiwiooilss", distSolver);
		}
		
		@Override
		public DistQuantifierEliminationOnBodyInWhichIndexOnlyOccursInsideLiteralsStepSolver clone() {
			DistributedQuantifierEliminationStepSolver cloneDistSolver = new DistributedQuantifierEliminationStepSolver(super.clone(), distSolver.actorRefFactory, distSolver.localLog);
			return (DistQuantifierEliminationOnBodyInWhichIndexOnlyOccursInsideLiteralsStepSolver) cloneDistSolver.getLocalWrappedQuantifierEliminationStepSolver();
		}
		
		@Override
		protected Expression solveSubProblems(AbstractQuantifierEliminationStepSolver subProblem1, AbstractQuantifierEliminationStepSolver subProblem2, Context context) {
			Pair<Expression, Expression> solutions = DistributedQuantifierEliminationStepSolver.solveSubProblems(subProblem1, subProblem2, context, distSolver);
			Expression result = combine(solutions.first, solutions.second, context);
			return result;
		}
	}
	
	public static class CreatorForSummationOnDifferenceArithmeticAndPolynomialStepSolver
			extends CreatorForDistributedQuantifierEliminationStepSolver {
		private static final long serialVersionUID = 1L;
		private transient DistSummationOnDifferenceArithmeticAndPolynomialStepSolver localSolver;
		
		public CreatorForSummationOnDifferenceArithmeticAndPolynomialStepSolver(
				SummationOnDifferenceArithmeticAndPolynomialStepSolver localSolver) {
			super(localSolver.getGroup(), localSolver.getIndexConstraint(), localSolver.getBody());
			if (localSolver instanceof DistSummationOnDifferenceArithmeticAndPolynomialStepSolver) {
				this.localSolver = (DistSummationOnDifferenceArithmeticAndPolynomialStepSolver) localSolver;
			}
		}

		@Override
		public QuantifierEliminationStepSolver create() {
			QuantifierEliminationStepSolver result;
			if (this.localSolver != null) {				
				result = this.localSolver;
				this.localSolver = null;
			}
			else {
				result = new DistSummationOnDifferenceArithmeticAndPolynomialStepSolver(distSolver, new DistSummationOnDifferenceArithmeticAndPolynomialStepSolver(distSolver, indexConstraint, body));
			}
			return result;
		}
	}
	
	public static class DistSummationOnDifferenceArithmeticAndPolynomialStepSolver extends SummationOnDifferenceArithmeticAndPolynomialStepSolver {
		private transient DistributedQuantifierEliminationStepSolver distSolver;
		public DistSummationOnDifferenceArithmeticAndPolynomialStepSolver(DistributedQuantifierEliminationStepSolver distSolver, SummationOnDifferenceArithmeticAndPolynomialStepSolver localSolver) {
			this(distSolver, localSolver.getIndexConstraint(), localSolver.getBody());
		}
		
		public DistSummationOnDifferenceArithmeticAndPolynomialStepSolver(DistributedQuantifierEliminationStepSolver distSolver, SingleVariableConstraint indexConstraint, Expression body) {
			super(indexConstraint, body);
			this.distSolver = distSolver;
		}
		
		@Override
		public Expression solve(Context context) {
			return DistributedQuantifierEliminationStepSolver.solve(context, "sodaapss", distSolver);
		}
		
		@Override
		public DistSummationOnDifferenceArithmeticAndPolynomialStepSolver clone() {
			DistributedQuantifierEliminationStepSolver cloneDistSolver = new DistributedQuantifierEliminationStepSolver(super.clone(), distSolver.actorRefFactory, distSolver.localLog);
			return (DistSummationOnDifferenceArithmeticAndPolynomialStepSolver) cloneDistSolver.getLocalWrappedQuantifierEliminationStepSolver();
		}
		
		@Override
		protected AbstractQuantifierEliminationStepSolver makeWithNewIndexConstraint(SingleVariableConstraint newIndexConstraint) {
			DistributedQuantifierEliminationStepSolver newDistSolver = new DistributedQuantifierEliminationStepSolver(super.makeWithNewIndexConstraint(newIndexConstraint), distSolver.actorRefFactory, distSolver.localLog);
			return (DistSummationOnDifferenceArithmeticAndPolynomialStepSolver) newDistSolver.getLocalWrappedQuantifierEliminationStepSolver();
		}
		
		@Override
		protected Expression solveSubProblems(AbstractQuantifierEliminationStepSolver subProblem1, AbstractQuantifierEliminationStepSolver subProblem2, Context context) {
			Pair<Expression, Expression> solutions = DistributedQuantifierEliminationStepSolver.solveSubProblems(subProblem1, subProblem2, context, distSolver);
			Expression result = combine(solutions.first, solutions.second, context);
			return result;
		}
	}

	public static class CreatorSummationOnLinearRealArithmeticAndPolynomialStepSolver
			extends CreatorForDistributedQuantifierEliminationStepSolver {
		private static final long serialVersionUID = 1L;
		private transient DistSummationOnLinearRealArithmeticAndPolynomialStepSolver localSolver;

		public CreatorSummationOnLinearRealArithmeticAndPolynomialStepSolver(
				SummationOnLinearRealArithmeticAndPolynomialStepSolver localSolver) {
			super(localSolver.getGroup(), localSolver.getIndexConstraint(), localSolver.getBody());
			if (localSolver instanceof DistSummationOnLinearRealArithmeticAndPolynomialStepSolver) {
				this.localSolver = (DistSummationOnLinearRealArithmeticAndPolynomialStepSolver) localSolver;
			}
		}

		@Override
		public QuantifierEliminationStepSolver create() {
			QuantifierEliminationStepSolver result;
			if (this.localSolver != null) {				
				result = this.localSolver;
				this.localSolver = null;
			}
			else {
				result = new DistSummationOnLinearRealArithmeticAndPolynomialStepSolver(this.distSolver, new DistSummationOnLinearRealArithmeticAndPolynomialStepSolver(distSolver, indexConstraint, body));
			}
			return result;
		}
	}	
	
	public static class DistSummationOnLinearRealArithmeticAndPolynomialStepSolver extends SummationOnLinearRealArithmeticAndPolynomialStepSolver {
		private transient DistributedQuantifierEliminationStepSolver distSolver;
		public DistSummationOnLinearRealArithmeticAndPolynomialStepSolver(DistributedQuantifierEliminationStepSolver distSolver, SummationOnLinearRealArithmeticAndPolynomialStepSolver localSolver) {
			this(distSolver, localSolver.getIndexConstraint(), localSolver.getBody());
		}
		
		public DistSummationOnLinearRealArithmeticAndPolynomialStepSolver(DistributedQuantifierEliminationStepSolver distSolver, SingleVariableConstraint indexConstraint, Expression body) {
			super(indexConstraint, body);
			this.distSolver = distSolver;
		}
		
		@Override
		public Expression solve(Context context) {
			return DistributedQuantifierEliminationStepSolver.solve(context, "solraapss", distSolver);
		}
		
		@Override
		public DistSummationOnLinearRealArithmeticAndPolynomialStepSolver clone() {
			DistributedQuantifierEliminationStepSolver cloneDistSolver = new DistributedQuantifierEliminationStepSolver(super.clone(), distSolver.actorRefFactory, distSolver.localLog);
			return (DistSummationOnLinearRealArithmeticAndPolynomialStepSolver) cloneDistSolver.getLocalWrappedQuantifierEliminationStepSolver();
		}
		
		@Override
		protected AbstractQuantifierEliminationStepSolver makeWithNewIndexConstraint(SingleVariableConstraint newIndexConstraint) {
			DistributedQuantifierEliminationStepSolver newDistSolver = new DistributedQuantifierEliminationStepSolver(super.makeWithNewIndexConstraint(newIndexConstraint), distSolver.actorRefFactory, distSolver.localLog);
			return (DistSummationOnLinearRealArithmeticAndPolynomialStepSolver) newDistSolver.getLocalWrappedQuantifierEliminationStepSolver();
		}
		
		@Override
		protected Expression solveSubProblems(AbstractQuantifierEliminationStepSolver subProblem1, AbstractQuantifierEliminationStepSolver subProblem2, Context context) {
			Pair<Expression, Expression> solutions = DistributedQuantifierEliminationStepSolver.solveSubProblems(subProblem1, subProblem2, context, distSolver);
			Expression result = combine(solutions.first, solutions.second, context);
			return result;
		}
	}
}