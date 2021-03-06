package com.sri.ai.distributed.sgdpllt.actor;

import static com.sri.ai.util.Util.myAssert;

import com.sri.ai.distributed.sgdpllt.message.ContextDependentExpressionProblem;
import com.sri.ai.distributed.sgdpllt.message.ContextDependentExpressionSolution;
import com.sri.ai.distributed.sgdpllt.util.AkkaUtil;
import com.sri.ai.distributed.sgdpllt.util.TestSerialize;
import com.sri.ai.expresso.api.Expression;
import com.sri.ai.grinder.sgdpllt.api.Context;
import com.sri.ai.grinder.sgdpllt.api.StepSolver;
import com.sri.ai.grinder.sgdpllt.core.constraint.ContextSplitting;
import com.sri.ai.grinder.sgdpllt.library.controlflow.IfThenElse;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Creator;
import akka.pattern.Patterns;
import scala.concurrent.Await;
import scala.concurrent.Future;

// TODO - this code as designed is blocking, which is non-optimal but required to work with the pre-existing logic in aic-expresso.
// Ideally, messages should be sent and received asynchronously throughout the call hierarchy.	
public class ContextDependentExpressionProblemSolverActor extends UntypedActor {
	
	LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	public static Props props() {
		return Props.create(new Creator<ContextDependentExpressionProblemSolverActor>() {
			private static final long serialVersionUID = 1L;
			@Override
			public ContextDependentExpressionProblemSolverActor create() {
				return new ContextDependentExpressionProblemSolverActor();
			}
		});
	}

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
		// NOTE: This must be called first before it can be acted upon.
		problem.setLocalActorInfo(getContext(), log);
		
		StepSolver<Expression> stepSolver = problem.getLocalStepSolver();
		Context context = problem.getLocalContext();	
		
		log.debug("CDEPS-solve:stepSolver={}", stepSolver);
		
		Expression result;
		StepSolver.SolverStep<Expression> step = stepSolver.step(context);
		
		log.debug("CDEPS-solve:step result={}", step);
		if (step.itDepends()) {	
			final Expression splitOnLiteral = step.getSplitter();
			ContextSplitting split = (ContextSplitting) step.getContextSplitting();
			myAssert(() -> split.isUndefined(), () -> "Undefined " + ContextSplitting.class + " result value: " + split.getResult());

			ActorRef subSolver1 = getContext().actorOf(props());
			ActorRef subSolver2 = getContext().actorOf(props());

			Future<Object> subSolutionFuture1 = Patterns.ask(subSolver1, TestSerialize.serializeMessage(problem.createSubProblem(step.getStepSolverForWhenSplitterIsTrue(), split.getConstraintAndLiteral()), log), AkkaUtil.getDefaultTimeout());
			Future<Object> subSolutionFuture2 = Patterns.ask(subSolver2, TestSerialize.serializeMessage(problem.createSubProblem(step.getStepSolverForWhenSplitterIsFalse(), split.getConstraintAndLiteralNegation()), log), AkkaUtil.getDefaultTimeout());
						
			// NOTE - can alternatively use subSolutionFuture1.zip(subSolutionFuture2).map(...) so only one Await.result call required.
			// However, as this is prototype code and we don't want to use Await.result at all in the final version we will leave as is
			// for the time being.
			ContextDependentExpressionSolution subSolution1 = (ContextDependentExpressionSolution) Await.result(subSolutionFuture1, AkkaUtil.getDefaultTimeout().duration());
			ContextDependentExpressionSolution subSolution2 = (ContextDependentExpressionSolution) Await.result(subSolutionFuture2, AkkaUtil.getDefaultTimeout().duration());
			
			result =  IfThenElse.make(splitOnLiteral, subSolution1.getLocalValue(), subSolution2.getLocalValue(), true);

			log.debug("CDEPS-solve itDepends:result={}", result);	
		}
		else {				
			result = step.getValue();
			log.debug("CDEPS-solve solution:result={}", result);
		}
		
		getSender().tell(new ContextDependentExpressionSolution(result), getSelf());
		
		// Should only be called once, after which stop self.
		getContext().stop(getSelf());
	}
}