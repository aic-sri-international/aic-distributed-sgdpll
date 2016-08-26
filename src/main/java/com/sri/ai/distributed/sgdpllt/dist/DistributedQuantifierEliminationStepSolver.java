package com.sri.ai.distributed.sgdpllt.dist;

import java.util.concurrent.TimeUnit;

import com.sri.ai.distributed.sgdpllt.message.ContextDependentExpressionSolution;
import com.sri.ai.distributed.sgdpllt.wrapper.QuantifierEliminationStepSolverWrapper;
import com.sri.ai.expresso.api.Expression;
import com.sri.ai.grinder.sgdpllt.api.Context;
import com.sri.ai.grinder.sgdpllt.core.solver.QuantifierEliminationStepSolver;

import akka.actor.ActorRef;
import akka.japi.Creator;
import akka.pattern.Patterns;
import akka.util.Timeout;
import scala.concurrent.Await;
import scala.concurrent.Future;

public class DistributedQuantifierEliminationStepSolver extends QuantifierEliminationStepSolverWrapper {
	private static final long serialVersionUID = 1L;
	
	// TODO - make configurable
	private static final Timeout _defaultTimeout = new Timeout(60, TimeUnit.SECONDS); 
	
	
	private transient ActorRef contextDependentExpressionProblemSolverActor;
	
	public DistributedQuantifierEliminationStepSolver(QuantifierEliminationStepSolver localQuantifierEliminatorStepSolver,  ActorRef contextDependentExpressionProblemSolverActor) {
		super(constructCreator(localQuantifierEliminatorStepSolver));
		this.contextDependentExpressionProblemSolverActor =  contextDependentExpressionProblemSolverActor;
	}
	
	@Override
	public Expression solve(Context context) {
		Expression result;
		
		// TODO - null argument need to be a QuantifierProblem message.
		Future<Object> futureResult = Patterns.ask(contextDependentExpressionProblemSolverActor, null, _defaultTimeout);
		
		try {
//TODO - ideally, do not want to use blocking but have to for the time being to work with existing aic-expresso control flow.				
			ContextDependentExpressionSolution solution = (ContextDependentExpressionSolution) Await.result(futureResult, _defaultTimeout.duration());
			result = solution.getLocalValue();
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
		
		return result;
	}
	
// TODO - I need to handle cloning carefully, so that a new creator is constructed for the clone and not the original instance.
	
		
	public static Creator<QuantifierEliminationStepSolver> constructCreator(QuantifierEliminationStepSolver localQuantifierEliminatorStepSolver) {
		return null; // TODO
	}
}