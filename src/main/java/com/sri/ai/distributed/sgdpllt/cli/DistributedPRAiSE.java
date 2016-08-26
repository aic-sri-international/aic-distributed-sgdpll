package com.sri.ai.distributed.sgdpllt.cli;

import java.util.function.Supplier;

import com.sri.ai.distributed.sgdpllt.actor.ContextDependentExpressionProblemSolverActor;
import com.sri.ai.distributed.sgdpllt.dist.TheoryWithDistributedQuantifierEliminatorStepSolvers;
import com.sri.ai.grinder.sgdpllt.api.Theory;
import com.sri.ai.grinder.sgdpllt.theory.compound.CompoundTheory;
import com.sri.ai.grinder.sgdpllt.theory.differencearithmetic.DifferenceArithmeticTheory;
import com.sri.ai.grinder.sgdpllt.theory.equality.EqualityTheory;
import com.sri.ai.grinder.sgdpllt.theory.linearrealarithmetic.LinearRealArithmeticTheory;
import com.sri.ai.grinder.sgdpllt.theory.propositional.PropositionalTheory;
import com.sri.ai.praise.sgsolver.cli.PRAiSE;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.japi.Creator;

public class DistributedPRAiSE extends PRAiSE {

	public static void main(String[] args) {
		run(args, new Supplier<Theory>() {
			@Override
			public Theory get() {
				try {
					ActorSystem system = ActorSystem.create("dsgdpllt");
					ActorRef rootActor = system.actorOf(ContextDependentExpressionProblemSolverActor.props());
					
					return new TheoryWithDistributedQuantifierEliminatorStepSolvers(new DefaultTheoryCreator(), rootActor);
				} catch (Exception ex) {
					throw new RuntimeException(ex);
				}
			}
		});
	}

	public static class DefaultTheoryCreator implements Creator<Theory> {
		private static final long serialVersionUID = 1L;

		@Override
		public Theory create() throws Exception {
			return new CompoundTheory(new EqualityTheory(false, true), new DifferenceArithmeticTheory(false, true),
					new LinearRealArithmeticTheory(false, true), new PropositionalTheory());
		}
	}
}
