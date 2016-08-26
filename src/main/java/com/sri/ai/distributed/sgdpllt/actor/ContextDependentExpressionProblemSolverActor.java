package com.sri.ai.distributed.sgdpllt.actor;

import static com.sri.ai.util.Util.myAssert;

import java.util.concurrent.TimeUnit;

import com.sri.ai.distributed.sgdpllt.message.ContextDependentExpressionProblem;
import com.sri.ai.distributed.sgdpllt.message.ContextDependentExpressionSolution;
import com.sri.ai.expresso.api.Expression;
import com.sri.ai.grinder.sgdpllt.api.Context;
import com.sri.ai.grinder.sgdpllt.api.ContextDependentProblemStepSolver;
import com.sri.ai.grinder.sgdpllt.core.constraint.ContextSplitting;
import com.sri.ai.grinder.sgdpllt.library.controlflow.IfThenElse;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorContext;
import akka.dispatch.Mapper;
import akka.japi.Creator;
import akka.pattern.Patterns;
import akka.util.Timeout;
import scala.concurrent.Await;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;

//TODO - this code as designed is blocking, which is non-optimal but required to work with the pre-existing logic in aic-expresso.
//Ideally, messages should be sent and received asynchronously throughout the call hierarchy.	
public class ContextDependentExpressionProblemSolverActor extends UntypedActor {
	
	public static Props props() {
		return Props.create(new Creator<ContextDependentExpressionProblemSolverActor>() {
			private static final long serialVersionUID = 1L;
			@Override
			public ContextDependentExpressionProblemSolverActor create() {
				return new ContextDependentExpressionProblemSolverActor();
			}
		});
	}
	
	// TODO - make configurable
	private static final Timeout _defaultTimeout = new Timeout(60, TimeUnit.SECONDS); 

	@Override
	public void onReceive(Object message) throws Exception {
		if (message instanceof ContextDependentExpressionProblem) {
			solve((ContextDependentExpressionProblem)message);
		}
		else {
			unhandled(message);
		}
	}
	
	protected void solve(ContextDependentExpressionProblem problem) throws Exception {		
		ContextDependentProblemStepSolver<Expression> stepSolver = problem.getLocalStepSolver();
		Context context = problem.getLocalContext();
		
		Expression result;
		ContextDependentProblemStepSolver.SolverStep<Expression> step = stepSolver.step(context);
		if (step.itDepends()) {
			final Expression splitOnLiteral = step.getLiteral();
			ContextSplitting split = (ContextSplitting) step.getContextSplitting();
			myAssert(() -> split.isUndefined(), () -> "Undefined " + ContextSplitting.class + " result value: " + split.getResult());
			final ExecutionContext ec = getContext().dispatcher();
			final ActorRef subSolver1 = getContext().actorOf(props());
			final ActorRef subSolver2 = getContext().actorOf(props());
			final UntypedActorContext actorContext = getContext();
			Future<Object> subSolutionFuture1 = Patterns.ask(subSolver1, problem.createSubProblem(step.getStepSolverForWhenLiteralIsTrue(), split.getConstraintAndLiteral()), _defaultTimeout);
			Future<Object> subSolutionFuture2 = Patterns.ask(subSolver2, problem.createSubProblem(step.getStepSolverForWhenLiteralIsFalse(), split.getConstraintAndLiteralNegation()), _defaultTimeout);
			Future<Expression> resultFuture = subSolutionFuture1.zip(subSolutionFuture2).map(new Mapper<scala.Tuple2<Object, Object>, Expression>() {
				@Override
				public Expression apply(scala.Tuple2<Object, Object> zipped) {
					ContextDependentExpressionSolution subSolution1 = (ContextDependentExpressionSolution) zipped._1;
					ContextDependentExpressionSolution subSolution2 = (ContextDependentExpressionSolution) zipped._2;
					Expression combinedSolution = IfThenElse.make(splitOnLiteral, subSolution1.getLocalValue(), subSolution2.getLocalValue(), true);
					
					// These are once off calls, so ensure we clean up.
					actorContext.stop(subSolver1);
					actorContext.stop(subSolver2);
					
					return combinedSolution;
				}
			}, ec);
			
			result = Await.result(resultFuture, _defaultTimeout.duration());
		}
		else {
			result = step.getValue();
		}
		
		getSender().tell(new ContextDependentExpressionSolution(result), getSelf());
	}
}