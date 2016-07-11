package com.sri.ai.distributed.experiment;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.spark.api.java.JavaRDD;

import com.sri.ai.distributed.util.RunInSparkContext;
import com.sri.ai.expresso.api.Expression;
/*
import com.sri.ai.grinder.api.RewritingProcess;
import com.sri.ai.grinder.plaindpll.api.Constraint;
import com.sri.ai.grinder.plaindpll.api.GroupProblemType;
import com.sri.ai.grinder.plaindpll.api.InputTheory;
import com.sri.ai.grinder.plaindpll.api.Solver;
import com.sri.ai.grinder.plaindpll.core.AbstractSolver;
import com.sri.ai.grinder.plaindpll.core.SGDPLLT;
import com.sri.ai.grinder.plaindpll.core.SGDPLLTParallelizer;
*/
public class SparkSGDPLLTMapReduceExperiment { 
/* extends AbstractSolver {


	public SparkSGDPLLTMapReduceExperiment(InputTheory inputTheory, GroupProblemType problemType) {
		super(inputTheory, problemType);
	}
	
	@Override
	protected Expression solveAfterBookkeeping(Expression expression, Collection<Expression> indices, Constraint constraint, RewritingProcess process) {
		int depth = 3; // TODO - make configurable
				
		final InputTheory      inputTheory = getInputTheory();
		final GroupProblemType problemType = getProblemType();
		final List<SubProblem> subProblems = new ArrayList<>();
		
// TODO - what about sub-problems/sub-branches that don't meet the depth that the collector is not called?		
		SGDPLLTParallelizer.Collector collector =
				(e, i, c, p) -> {
					subProblems.add(new SubProblem(e,i,c,p));
				};
				
		SGDPLLTParallelizer parallelizer = new SGDPLLTParallelizer(getInputTheory(), getProblemType(), collector, depth);
		
		// This call will initialize subProblems
		parallelizer.solve(expression, indices, constraint, process);		
		
		System.out.println("#sub-problems="+subProblems.size());
		
		
		Expression result = RunInSparkContext.run((sparkContext) -> {
			Expression answer = getProblemType().getGroup().additiveIdentityElement();
			
			JavaRDD<SubProblem> subProblemsRDD = sparkContext.parallelize(subProblems);
			
			JavaRDD<Expression> subSolutionsRDD = subProblemsRDD.map((sp) -> {
				Solver solver = new SGDPLLT(inputTheory, problemType);
				Expression subSolution = solver.solve(sp.expression, sp.indices, sp.constraint, sp.process);
				return subSolution;
			});
			
			answer = subSolutionsRDD.reduce((s1, s2) -> {
				return addSymbolicResults(s1, s2);
			});
			
			return answer;
		});
		
		return result;
	}
}

class SubProblem implements Serializable {
	public Expression             expression;
	public Collection<Expression> indices;
	public Constraint             constraint;
	public RewritingProcess       process;
	
	public SubProblem(Expression expression, Collection<Expression> indices, Constraint constraint, RewritingProcess process) {
		this.expression = expression;
		this.indices    = indices;
		this.constraint = constraint;
		this.process    = process;
	}
*/
}
