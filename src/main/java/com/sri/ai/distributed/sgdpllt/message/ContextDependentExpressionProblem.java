package com.sri.ai.distributed.sgdpllt.message;

import java.io.Serializable;

import com.sri.ai.expresso.api.Expression;
import com.sri.ai.grinder.sgdpllt.api.Context;
import com.sri.ai.grinder.sgdpllt.api.ContextDependentProblemStepSolver;

import akka.actor.ActorRef;

// NOTE: Immutable
public abstract class ContextDependentExpressionProblem implements Serializable {	
	private static final long serialVersionUID = 1L;
	
	public final ActorRef contextDependentExpressionProblemSolverActor;
	public final SerializableContext serializableContext; 
	
	// DON'T SERIALIZE
	
	
	public ContextDependentExpressionProblem(ActorRef contextDependentExpressionProblemSolverActor, Context context) {
		this.contextDependentExpressionProblemSolverActor = contextDependentExpressionProblemSolverActor;
		this.serializableContext = new SerializableContext(context);
	}
	
	public abstract ContextDependentExpressionProblem createSubProblem(ActorRef contextDependentExpressionProblemSolverActor, ContextDependentProblemStepSolver<Expression> stepSolver, Context localContext);	
	public abstract ContextDependentProblemStepSolver<Expression> getLocalStepSolver();
	
	public Context getLocalContext() {
		return serializableContext.getLocalContext();
	}
}
