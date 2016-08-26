package com.sri.ai.distributed.sgdpllt.dist;

import java.util.concurrent.TimeUnit;

import com.sri.ai.distributed.sgdpllt.actor.ContextDependentExpressionProblemSolverActor;
import com.sri.ai.distributed.sgdpllt.message.ContextDependentExpressionSolution;
import com.sri.ai.distributed.sgdpllt.message.QuantifierEliminationProblem;
import com.sri.ai.distributed.sgdpllt.wrapper.QuantifierEliminationStepSolverWrapper;
import com.sri.ai.expresso.api.Expression;
import com.sri.ai.grinder.sgdpllt.api.Context;
import com.sri.ai.grinder.sgdpllt.core.solver.QuantifierEliminationStepSolver;

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
		
	private ActorRefFactory actorRefFactory;
	private LoggingAdapter localLog;
	
	public DistributedQuantifierEliminationStepSolver(QuantifierEliminationStepSolver localQuantifierEliminatorStepSolver) {
		super(constructCreator(localQuantifierEliminatorStepSolver));
	}
	
	public DistributedQuantifierEliminationStepSolver(QuantifierEliminationStepSolver localQuantifierEliminatorStepSolver,  ActorRefFactory actorRefFactory, LoggingAdapter localLog) {
		super(constructCreator(localQuantifierEliminatorStepSolver));
		setActorRefFactory(actorRefFactory);
		setLocalLog(localLog);
	}
	
	public void setActorRefFactory(ActorRefFactory actorRefFactory) {
		this.actorRefFactory = actorRefFactory;
	}
	
	public void setLocalLog(LoggingAdapter localLog) {
		this.localLog = localLog;
	}
	
	@Override
	public Expression solve(Context context) {
		Expression result;
		localLog.debug("DQEL-solve:idx={}, idx constraint={}, body={}", getIndex(), getIndexConstraint(), getBody());
		QuantifierEliminationProblem quantifierEliminationProblem = new QuantifierEliminationProblem(context, this);
		ActorRef contextDependentExpressionProblemSolverActor = actorRefFactory.actorOf(ContextDependentExpressionProblemSolverActor.props());
		Future<Object> futureResult = Patterns.ask(contextDependentExpressionProblemSolverActor, quantifierEliminationProblem, _defaultTimeout);
		try {
//TODO - ideally, do not want to use blocking but have to for the time being to work with existing aic-expresso control flow.				
			ContextDependentExpressionSolution solution = (ContextDependentExpressionSolution) Await.result(futureResult, _defaultTimeout.duration());
			result = solution.getLocalValue();
			
			// Ensure we clean up the actor.
			actorRefFactory.stop(contextDependentExpressionProblemSolverActor);
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
		
		return result;
	}
	
// TODO - I need to handle cloning carefully, so that a new creator is constructed for the clone and not the original instance.
	
		
	public static Creator<QuantifierEliminationStepSolver> constructCreator(final QuantifierEliminationStepSolver localQuantifierEliminatorStepSolver) {
// TODO - add proper support for creating quantifier eliminator step solvers based on information from a given local instance		
		return new Creator<QuantifierEliminationStepSolver>() {
			private static final long serialVersionUID = 1L;
			int cnt = 0;
			@Override
			public QuantifierEliminationStepSolver create() {
				cnt++;
				if (cnt > 1) {
					throw new IllegalStateException("Creating local QuantifierEliminationStepSolver more than once, currently not supported");
				}
// TODO - this is a hack!!!, should work in local process when no serialization is used.				
				return localQuantifierEliminatorStepSolver;
			}
		};
	}
}