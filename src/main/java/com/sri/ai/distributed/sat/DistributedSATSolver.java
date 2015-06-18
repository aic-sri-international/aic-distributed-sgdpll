/*
 * Copyright (c) 2015, SRI International
 * All rights reserved.
 * Licensed under the The BSD 3-Clause License;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 * 
 * http://opensource.org/licenses/BSD-3-Clause
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 
 * Neither the name of the aic-expresso nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, 
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.sri.ai.distributed.sat;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.broadcast.Broadcast;
import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.ISolver;

import com.sri.ai.distributed.sat.iterable.IterableAssumptions;
import com.sri.ai.distributed.util.RunInSparkContext;

/**
 * 
 * @author oreilly
 *
 */
public class DistributedSATSolver implements SATSolver, Serializable {
	private static final long serialVersionUID = 1L;
	
	public static final int NUMBER_VARIABLE_TO_PRE_ASSIGN = 8;
	
	@Override
	public int[] findModel(CNFProblem cnfProblem) {
		int[] result = null;
		
		if (cnfProblem.getNumberVariables() < NUMBER_VARIABLE_TO_PRE_ASSIGN+4) {
			result = new LocalSATSolver().findModel(cnfProblem);
		} 
		else {			
			result = RunInSparkContext.run(new Function<JavaSparkContext, int[]>() {
				public int[] apply(JavaSparkContext sparkContext) {
					int[] model = null;
					
					// NOTE: Spark does not support nested RDDs or performing Spark actions inside of transformations; see:
					// https://issues.apache.org/jira/browse/SPARK-5063
					// Therefore, we will move the cnf problem's clauses to a broadcast variable, so that each partition
					// can access the information easily
					int     numVars    = (int)cnfProblem.getNumberVariables();
					int     numClauses = (int)cnfProblem.getNumberClauses();
					int[][] clauses    = new int[numClauses][];
					int     i          = 0;
					Iterator<int[]> clauseIterator = cnfProblem.getClauses().toLocalIterator();
					while (clauseIterator.hasNext()) {
						int[] clause = clauseIterator.next();
						clauses[i] = clause;
						i++;
					}
					
					Broadcast<int[][]> clausesBroadcastVar = sparkContext.broadcast(clauses);
					
					// TODO - this is a hack and will only work in local mode (i.e. will fail on a cluster)
					// need a mechanism to signal to each job on the cluster that they should stop processing
					// once a single answer is found.
					AtomicBoolean foundModelSignal = new AtomicBoolean();
					
					// TODO - create array of iterators, size = sparkContext.defaultParallelism(), whereby
					// each iterator is over a partition of the the pre-assigned assumption values.
					JavaRDD<int[]> assumptions = sparkContext.parallelize(sparkContext.parallelize(Arrays.asList(0)).flatMap(zero -> { 
						return new IterableAssumptions(NUMBER_VARIABLE_TO_PRE_ASSIGN);
					}).collect());
									
					// Now determine if satisfiable by having each partition constrain their problem
					// based on the given assumptions.
					JavaRDD<int[]> models = assumptions.mapPartitions(partitionedAssumptions -> {
						List<int[]> foundModel = new ArrayList<>();
// TODO - remove					
System.out.println("enter mapParitition :"+Thread.currentThread());
						
						int[][] localClauses = clausesBroadcastVar.getValue();
						
						ISolver sat4jSolver = SolverFactory.newDefault();	
						
						sat4jSolver.newVar(numVars);
						for (int c = 0; c < numClauses; c++) {
							try {	
								int[] clause = localClauses[c];
								VecInt vClause = new VecInt(clause);				
								sat4jSolver.addClause(vClause);
							} catch (ContradictionException cex) {
								return foundModel; // no models exist
							}
						}
						
						int trys = 0;
						while (partitionedAssumptions.hasNext()) {
							if (foundModelSignal.get()) {
								break; // someone has found an answer
							}
							int[]  localAssumptions = partitionedAssumptions.next();
							VecInt vecAssumptions   = new VecInt(localAssumptions);
							int[]  localFindResult  = sat4jSolver.findModel(vecAssumptions);

// TODO - remove							
if (trys == 0) {System.out.println("mapPartition first assumptions="+vecAssumptions+", "+Thread.currentThread()); }
							
							trys++;
							if (localFindResult != null) {
								foundModelSignal.set(true);
// TODO - remove
System.out.println("mapParitition - found model in:"+Thread.currentThread()+", trys="+trys+", assumptions="+vecAssumptions);
								// We are only interested in the first one we find
								foundModel.add(localFindResult);
								break;
							}
						}
							
						return foundModel;
					});
					List<int[]> collectedModels = models.collect();
					if (collectedModels.size() > 0) {
						model = collectedModels.get(0);
					}
					
					return model;
				}
			});
		}
		
		return result;
	}
	
	@Override
	public int[] findModel(CNFProblem cnfProblem, int[] assumptions) {
		throw new UnsupportedOperationException("findModel() with assumptions not supported by distributed SAT");
	}
}
