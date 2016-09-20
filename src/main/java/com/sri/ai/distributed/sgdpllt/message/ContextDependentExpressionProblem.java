package com.sri.ai.distributed.sgdpllt.message;

import java.io.Serializable;

import com.sri.ai.distributed.sgdpllt.dist.DistributedTheory;
import com.sri.ai.expresso.api.Expression;
import com.sri.ai.grinder.sgdpllt.api.Context;
import com.sri.ai.grinder.sgdpllt.api.StepSolver;

import akka.actor.ActorRefFactory;
import akka.event.LoggingAdapter;

// NOTE: Immutable
public abstract class ContextDependentExpressionProblem implements Serializable {	
	private static final long serialVersionUID = 1L;
	
	public final SerializableContext serializableContext; 
	
	// DON'T SERIALIZE
	
	
	public ContextDependentExpressionProblem(Context context) {
		// NOTE: Ensure cloned so that theory is also cloned properly
		this.serializableContext = new SerializableContext(context.clone());
	}
	
	public void setLocalActorInfo(ActorRefFactory actorRefFactory, LoggingAdapter actorLog) {
		((DistributedTheory)getLocalContext().getTheory()).setLocalActorInfo(actorRefFactory, actorLog);
	}
	
	public abstract ContextDependentExpressionProblem createSubProblem(StepSolver<Expression> stepSolver, Context localContext);	
	public abstract StepSolver<Expression> getLocalStepSolver();
	
	public Context getLocalContext() {
		return serializableContext.getLocalContext();
	}
}
