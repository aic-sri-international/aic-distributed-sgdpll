package com.sri.ai.distributed.sgdpllt.wrapper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.sri.ai.expresso.api.Expression;
import com.sri.ai.expresso.api.Type;
import com.sri.ai.grinder.sgdpllt.api.Constraint;
import com.sri.ai.grinder.sgdpllt.api.Context;
import com.sri.ai.grinder.sgdpllt.api.ContextDependentExpressionProblemStepSolver;
import com.sri.ai.grinder.sgdpllt.api.SingleVariableConstraint;
import com.sri.ai.grinder.sgdpllt.api.Theory;
import com.sri.ai.grinder.sgdpllt.group.AssociativeCommutativeGroup;
import com.sri.ai.grinder.sgdpllt.simplifier.api.MapBasedTopSimplifier;

import akka.japi.Creator;

// NOTE: Immutable
public class TheoryWrapper implements Theory, Serializable {
	private static final long serialVersionUID = 1L;
	
	private final Creator<Theory> theoryCreator;
	
	// DON'T SERIALIZE	
	private transient Theory wrappedTheory;
	
	public TheoryWrapper(Creator<Theory> theoryCreator) throws Exception {
		this.theoryCreator = theoryCreator;
	}
	
	//
	// START - Theory
	@Override
	public Expression simplify(Expression expression, Context context) {
		return getLocalWrappedTheory().simplify(expression, context);
	}
	
	@Override
	public MapBasedTopSimplifier getMapBasedTopSimplifier() {
		return getLocalWrappedTheory().getMapBasedTopSimplifier();
	}
	
	@Override
	public boolean isSuitableFor(Expression variable, Type type) {
		return wrappedTheory.isSuitableFor(variable, type);
	}
	
	@Override
	public boolean isLiteral(Expression expression, Context context) {
		return getLocalWrappedTheory().isLiteral(expression, context);
	}
	
	@Override
	public boolean isConjunctiveClause(Expression formula, Context context) {
		return getLocalWrappedTheory().isConjunctiveClause(formula, context);
	}
	
	@Override
	public boolean isNonTrivialAtom(Expression expression, Context context) {
		return getLocalWrappedTheory().isNonTrivialAtom(expression, context);
	}
	
	@Override
	public boolean isNonTrivialLiteral(Expression expression, Context context) {
		return getLocalWrappedTheory().isNonTrivialLiteral(expression, context);
	}
	
	@Override
	public boolean isNonTrivialNegativeLiteral(Expression expression, Context context) {
		return getLocalWrappedTheory().isNonTrivialNegativeLiteral(expression, context);
	}
	
	@Override
	public SingleVariableConstraint makeSingleVariableConstraint(Expression variable, Theory theory, Context context) {
		return getLocalWrappedTheory().makeSingleVariableConstraint(variable, theory, context);
	}
	
	@Override
	public Constraint makeTrueConstraint() {
		return getLocalWrappedTheory().makeTrueConstraint();
	}
	
	@Override
	public boolean singleVariableConstraintIsCompleteWithRespectToItsVariable() {
		return getLocalWrappedTheory().singleVariableConstraintIsCompleteWithRespectToItsVariable();
	}
	
	@Override
	public boolean isInterpretedInThisTheoryBesidesBooleanConnectives(Expression expression, Context context) {
		return getLocalWrappedTheory().isInterpretedInThisTheoryBesidesBooleanConnectives(expression, context);
	}
	
	@Override
	public ContextDependentExpressionProblemStepSolver getSingleVariableConstraintSatisfiabilityStepSolver(SingleVariableConstraint constraint, Context context) {
		return getLocalWrappedTheory().getSingleVariableConstraintSatisfiabilityStepSolver(constraint, context);
	}
	
	@Override
	public ContextDependentExpressionProblemStepSolver getSingleVariableConstraintModelCountingStepSolver(SingleVariableConstraint constraint, Context context) {
		return getLocalWrappedTheory().getSingleVariableConstraintModelCountingStepSolver(constraint, context);
	}
	
	@Override
	public ContextDependentExpressionProblemStepSolver getSingleVariableConstraintQuantifierEliminatorStepSolver(AssociativeCommutativeGroup group, SingleVariableConstraint constraint, Expression currentBody, Context context) {
		return getLocalWrappedTheory().getSingleVariableConstraintQuantifierEliminatorStepSolver(group, constraint, currentBody, context);
	}
	
	@Override
	public Expression getLiteralNegation(Expression literal, Context context) {
		return getLocalWrappedTheory().getLiteralNegation(literal, context);
	}
	
	@Override
	public Collection<Expression> getVariablesIn(Expression expression, Context context) {
		return getLocalWrappedTheory().getVariablesIn(expression, context);
	}
	
	@Override
	public boolean isVariable(Expression expression, Context context) {
		return getLocalWrappedTheory().isVariable(expression, context);
	}
	
	////////////AUTOMATIC TESTING
	@Override
	public void setVariableNamesAndTypesForTesting(Map<String, Type> variableNamesForTesting) {
		getLocalWrappedTheory().setVariableNamesAndTypesForTesting(variableNamesForTesting);
	}
	
	@Override
	public Map<String, Type> getVariableNamesAndTypesForTesting() {
		return getLocalWrappedTheory().getVariableNamesAndTypesForTesting();
	}
	
	@Override
	public List<String> getVariableNamesForTesting() {
		return getLocalWrappedTheory().getVariableNamesForTesting();
	}
	
	@Override
	public ArrayList<Expression> getVariablesForTesting() {
		return getLocalWrappedTheory().getVariablesForTesting();
	}
	
	@Override
	public Collection<Type> getTypesForTesting() {
		return getLocalWrappedTheory().getTypesForTesting();
	}
	
	@Override
	public Collection<Type> getNativeTypes() {
		return getLocalWrappedTheory().getNativeTypes();
	}
	
	@Override
	public String pickTestingVariableAtRandom(Random random) {
		return getLocalWrappedTheory().pickTestingVariableAtRandom(random);
	}
	
	@Override
	public Expression makeRandomAtomOn(String variable, Random random, Context context) {
		return getLocalWrappedTheory().makeRandomAtomOn(variable, random, context);
	}
	
	@Override
	public Expression makeRandomAtom(Random random, Context context) {
		return getLocalWrappedTheory().makeRandomAtom(random, context);
	}
	
	@Override
	public Expression makeRandomAtomOnTestingVariable(Random random, Context context) {
		return getLocalWrappedTheory().makeRandomAtomOnTestingVariable(random, context);
	}
	
	@Override
	public Expression makeRandomLiteralOn(String variable, Random random, Context context) {
		return getLocalWrappedTheory().makeRandomLiteralOn(variable, random, context);
	}
	
	@Override
	public Expression makeRandomLiteral(Random random, Context context) {
		return getLocalWrappedTheory().makeRandomLiteral(random, context);
	}
	
	@Override
	public Context extendWithTestingInformation(Context context) {
		return getLocalWrappedTheory().extendWithTestingInformation(context);
	}
	
	@Override
	public Context makeContextWithTestingInformation() {
		return getLocalWrappedTheory().makeContextWithTestingInformation();
	}
	
	// END - Theory
	//
	
	protected Theory getLocalWrappedTheory() {
		if (wrappedTheory == null) {
			try {
				wrappedTheory = theoryCreator.create();
			}
			catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}
		return wrappedTheory;
	}
}