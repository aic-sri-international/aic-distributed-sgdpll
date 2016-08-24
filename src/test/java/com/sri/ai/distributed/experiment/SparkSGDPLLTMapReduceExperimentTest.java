package com.sri.ai.distributed.experiment;

import static com.sri.ai.expresso.helper.Expressions.apply;
import static com.sri.ai.expresso.helper.Expressions.parse;
import static com.sri.ai.grinder.sgdpllt.library.indexexpression.IndexExpressions.makeIndexExpression;
import static com.sri.ai.util.Util.list;
import static com.sri.ai.util.Util.mapIntoArrayList;
import static com.sri.ai.util.Util.toArrayList;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.sri.ai.expresso.api.Expression;
import com.sri.ai.expresso.api.IndexExpressionsSet;
import com.sri.ai.expresso.core.DefaultIntensionalMultiSet;
import com.sri.ai.expresso.core.ExtensionalIndexExpressionsSet;
import com.sri.ai.expresso.helper.Expressions;
import com.sri.ai.grinder.helper.GrinderUtil;
import com.sri.ai.grinder.sgdpllt.library.Associative;
import com.sri.ai.grinder.sgdpllt.library.Equality;
import com.sri.ai.grinder.sgdpllt.library.FunctorConstants;
import com.sri.ai.grinder.sgdpllt.library.boole.And;
import com.sri.ai.grinder.sgdpllt.library.boole.Or;
import com.sri.ai.grinder.sgdpllt.library.controlflow.IfThenElse;
import com.sri.ai.grinder.sgdpllt.library.number.GreaterThan;
import com.sri.ai.grinder.sgdpllt.library.number.LessThanOrEqualTo;
import com.sri.ai.grinder.sgdpllt.library.number.Minus;
import com.sri.ai.grinder.sgdpllt.library.number.Plus;
import com.sri.ai.grinder.sgdpllt.library.number.Times;
import com.typesafe.config.ConfigException.Parse;

public class SparkSGDPLLTMapReduceExperimentTest {
/*	
	protected static final Symbol DEFAULT_EVERYTHING_CARDINALITY_VALUE = makeSymbol(10);

	protected static final Expression everythingType = makeSymbol("Everything");

	protected static final Expression everythingCardinality = apply(CARDINALITY, everythingType);

	protected static class Parse implements Function<String, Expression> {
		@Override
		public Expression apply(String input) {
			return parse(input);
		}
	}

	protected void runSymbolicAndNonSymbolicTests(Expression expression, Collection<String> indicesStrings, Expression expected) {
		runSymbolicAndNonSymbolicTests(expression, indicesStrings, new HashMap<Expression, Expression>(), expected);
	}
	
	protected void runSymbolicAndNonSymbolicTests(
			Expression expression,
			Collection<String> indicesStrings,
			Map<Expression, Expression> providedFreeSymbolsTypes,
			Expression expected) {
		
		runTest(expression, indicesStrings, providedFreeSymbolsTypes, expected, true );
		
		RewritingProcess process = DirectCardinalityComputationFactory.newCardinalityProcess();
		
		process.putGlobalObject(everythingCardinality, DEFAULT_EVERYTHING_CARDINALITY_VALUE);
		
		TotalRewriter normalizer = new TotalRewriter(
				new PlainSubstitution(),
				new Associative("+"), new Associative("*"), new Associative("and"), new Associative("or"),
				new Plus(), new Minus(), new Times(), new GreaterThan(), new LessThanOrEqualTo(), new Equality(),
				new Or(), new And(),
				new IfThenElse(),
				new IfThenElseBranchesAreIdentical()
				);
		
		RewritingProcess subProcess = extendProcessWithProvidedTypesAndTypeEverythingForUnspecifiedFreeSymbols(expected, providedFreeSymbolsTypes, process);
		Expression expectedWithTypeSize = normalizer.rewrite(expected, subProcess);
		
		runTest(expression, indicesStrings, providedFreeSymbolsTypes, expectedWithTypeSize, false);
	}

	private RewritingProcess extendProcessWithProvidedTypesAndTypeEverythingForUnspecifiedFreeSymbols(Expression expression, Map<Expression, Expression> providedFreeSymbolsTypes, RewritingProcess process) {
		Map<Expression, Expression> freeSymbolsTypes = new LinkedHashMap<Expression, Expression>(providedFreeSymbolsTypes);
		for (Expression freeSymbol : Expressions.freeSymbols(expression, process)) {
			if ( ! freeSymbolsTypes.containsKey(freeSymbol)) {
				freeSymbolsTypes.put(freeSymbol, everythingType);
			}
		}
		process = GrinderUtil.extendContextualSymbols(freeSymbolsTypes, process);
		return process;
	}
	
	protected void runTest(
			Expression expression,
			Collection<String> indicesStrings,
			Map<Expression, Expression> providedFreeSymbolsTypes,
			Expression expected,
			boolean noTypeSize) {
		
		DefaultRewritingProcess process = new DefaultRewritingProcess(expression, null);
		
		Collection<Expression> indices;
		if (indicesStrings != null) {
			indices = mapIntoArrayList(indicesStrings, new Parse());
		}
		else {
			indices = getAllVariables(expression, process);
		}
		
		if (! noTypeSize) {
			process.putGlobalObject(everythingCardinality, DEFAULT_EVERYTHING_CARDINALITY_VALUE);
		}
		
		IndexExpressionsSet indexExpressions =
				new ExtensionalIndexExpressionsSet(
				indices
				.stream()
				.map(index -> makeIndexExpression(index, everythingType))
				.collect(toArrayList(indices.size())));
		
		Rewriter rewriter = makeRewriter();
		Expression problem = makeProblem(expression, indexExpressions);
		System.out.println("Problem: " + problem);
		RewritingProcess subProcess = extendProcessWithProvidedTypesAndTypeEverythingForUnspecifiedFreeSymbols(problem, providedFreeSymbolsTypes, process);

		Expression actual = rewriter.rewrite(problem, subProcess);
		System.out.println("Solution: " + actual);
		System.out.println("Expected: " + expected + "\n");
		Assert.assertEquals(expected, actual);
	}
	
	protected Rewriter makeRewriter() {
		return new SparkSGDPLLTMapReduceExperiment(new DefaultInputTheory(new EqualityConstraintTheory(new SymbolTermTheory())), new ModelCounting());
	}
	
	protected Expression makeProblem(Expression expression, IndexExpressionsSet indexExpressions) {
		Expression set = new DefaultIntensionalMultiSet(indexExpressions, Expressions.ONE, expression);
		Expression problem = apply(FunctorConstants.CARDINALITY, set);
		return problem;
	}

	@Test
	public void test() {
		
		Expression expression;
		Expression expected;
		Collection<String> indices;
		
		GrinderUtil.setTraceAndJustificationOffAndTurnOffConcurrency();

		// this example tests whether conditioning an index to a value considers previous disequalities on that index,
		// because X is split on b first, and then the algorithm attempts to condition on X = Y, but that requires Y to be != b.
		expression = parse("X != b and X = Y");
		indices    = list("X");
		expected   = parse("if Y = b then 0 else 1");
		runSymbolicAndNonSymbolicTests(expression, indices, expected);

		// tests elimination for quantified sub-expressions
		expression = parse("for all Y : X = Y");
		indices    = list("X");
		expected   = parse("if | type(Y) | - 1 = 0 then | Everything | else 0");
		runSymbolicAndNonSymbolicTests(expression, indices, expected);

		// tests case in which symbolic solutions with conditions that are not splitters need to be combined
		// Combination must discriminate between splitters and not splitters.
		// In this example, we get solutions with a condition on | Everything | - 1 > 0.
		expression = parse(""
				+ "(X = a and (Z = a and there exists Y in Everything : Y != b) or (Z != a and there exists Y in Everything : Y != c and Y != d))"
				+ "or"
				+ "(X != a and (Z = a and there exists Y in Everything : Y != e and Y != f and Y != g) or (Z != a and there exists Y in Everything : Y != c and Y != d))");
		indices    = list("X");
		expected   = parse("if Z = a then (if | Everything | - 1 > 0 then 1 else 0) + if | Everything | - 3 > 0 then | Everything | - 1 else 0 else (if | Everything | - 2 > 0 then 1 else 0) + if | Everything | - 2 > 0 then | Everything | - 1 else 0");
		runSymbolicAndNonSymbolicTests(expression, indices, expected);

		
		
		expression = parse("true");
		indices    = null; // means all variables
		expected   = parse("1");
		runSymbolicAndNonSymbolicTests(expression, indices, expected);

		expression = parse("false");
		indices    = null; // means all variables
		expected   = parse("0");
		runSymbolicAndNonSymbolicTests(expression, indices, expected);

		// tests answer completeness
		expression  = parse("(Y = a and X = T) or (Y != a and X = T1 and T = T1)");
		indices     = list("Y");
		// original algorithm provided this incomplete solution due to incomplete condition-applying-on-solution algorithm used in externalization
		// expected = parse("if X = T then if T = T1 then if T = T1 then 10 else 1 else 1 else (if X = T1 then if T = T1 then 9 else 0 else 0)");
		expected    = parse("if X = T then if T = T1 then | Everything | else 1 else 0");
		runSymbolicAndNonSymbolicTests(expression, indices, expected);

		
		
		expression = parse("X != Y");
		indices    = list("X");
		expected   = parse("| Everything | - 1");
		runSymbolicAndNonSymbolicTests(expression, indices, expected);
		
		expression = parse("X != Y and X != a");
		indices    = list("X");
		expected   = parse("if Y = a then | Everything | - 1 else | Everything | - 2");
		runSymbolicAndNonSymbolicTests(expression, indices, expected);
		
		expression = parse("X != Y and X != Z and X != a");
		indices    = list("X");
		expected   = parse("if Z = Y then if Y = a then | Everything | - 1 else | Everything | - 2 else if Y = a then | Everything | - 2 else if Z = a then | Everything | - 2 else | Everything | - 3");
		runSymbolicAndNonSymbolicTests(expression, indices, expected);
		
		expression = parse("Y = a and X != Y and X != a");
		indices    = list("X");
		expected   = parse("if Y = a then | Everything | - 1 else 0");
		runSymbolicAndNonSymbolicTests(expression, indices, expected);
		

		expression = parse("X1 != X2 and (X2 = X3 or X2 = X4) and X3 = X1 and X4 = X1");
		indices    = null; // means all variables
		expected   = parse("0");
		runSymbolicAndNonSymbolicTests(expression, indices, expected);
		
		expression = parse("X1 != X2 and X2 != X0 and X1 != X0");
		indices    = null; // means all variables
		expected   = parse("(| Everything | - 1) * | Everything | * (| Everything | - 2)");
		runSymbolicAndNonSymbolicTests(expression, indices, expected);
		
		expression = parse("true");
		indices    = null; // means all variables
		expected   = parse("1");
		runSymbolicAndNonSymbolicTests(expression, indices, expected);
		
		expression = parse("true");
		indices    = list("X", "Y");
		expected   = parse("| Everything | * | Everything |");
		runSymbolicAndNonSymbolicTests(expression, indices, expected);
		
		expression = parse("false");
		indices    = null; // means all variables
		expected   = parse("0");
		runSymbolicAndNonSymbolicTests(expression, indices, expected);
		
		expression = parse("false");
		indices    = list("X", "Y");
		expected   = parse("0");
		runSymbolicAndNonSymbolicTests(expression, indices, expected);
		
		
		expression = parse("X = a");
		indices    = null; // means all variables
		expected   = parse("1");
		runSymbolicAndNonSymbolicTests(expression, indices, expected);
		
		expression = parse("X != a");
		indices    = null; // means all variables
		expected   = parse("| Everything | - 1");
		runSymbolicAndNonSymbolicTests(expression, indices, expected);
		
		expression = parse("X = a");
		indices    = list("X", "Y");
		expected   = parse("| Everything |");
		runSymbolicAndNonSymbolicTests(expression, indices, expected);
		
		expression = parse("X != a");
		indices    = list("X", "Y");
		expected   = parse("(| Everything | - 1)*| Everything |");
		runSymbolicAndNonSymbolicTests(expression, indices, expected);
		
		expression = parse("X = a and Y != b");
		indices    = list("X", "Y");
		expected   = parse("| Everything | - 1");
		runSymbolicAndNonSymbolicTests(expression, indices, expected);
		
		expression = parse("X != a and Y != b");
		indices    = list("X", "Y");
		expected   = parse("(| Everything | - 1)*(| Everything | - 1)");
		runSymbolicAndNonSymbolicTests(expression, indices, expected);
		
		expression = parse("X != a or Y != b");
		indices    = list("X", "Y");
		expected   = parse("| Everything | + -1 + (| Everything | - 1) * | Everything |");
		runSymbolicAndNonSymbolicTests(expression, indices, expected);
		
		expression = parse("X != a and X != Y and Y != a");
		indices    = null;
		expected   = parse("(| Everything | - 2) * (| Everything | - 1)");
		runSymbolicAndNonSymbolicTests(expression, indices, expected);
	}
*/
}
