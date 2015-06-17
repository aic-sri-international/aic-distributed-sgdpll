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
package com.sri.ai.distributed.sat.reader;

import java.util.Iterator;
import java.util.StringJoiner;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.ISolver;

/**
 * Based on simplified version of DIMACS format described at:<br>
 * http://www.satcompetition.org/2009/format-benchmarks2009.html<br>
 * full format description can be found here:<br>
 * http://www.cs.ubc.ca/~hoos/SATLIB/benchm.html<br>
 * 
 * @author oreilly
 *
 */
public class SimplifiedDIMACSReader {

	
	public static void main(String[] args) {
		String    cnfFile = args[0];
		SparkConf conf    = new SparkConf().setAppName("Simplified DIMACS Reader").setMaster("local");
		
		try (JavaSparkContext sc = new JavaSparkContext(conf)) {
	    		    
	    	JavaRDD<String> cnfData = sc.textFile(cnfFile);
	    
	    	// Get the #variables and #clauses information from the problem definition line
	    	// e.g.
	    	// p cnf 5 3
	    	// has 5 varialbes and 3 clauses
	    	String[] problemInfo = cnfData.filter(line -> line.trim().startsWith("p")).toLocalIterator().next().split("\\s+");
	    	long numVariables = Long.parseLong(problemInfo[2]);
	    	long numClauses   = Long.parseLong(problemInfo[3]);
	    	
	    	JavaRDD<int[]> clauses = cnfData
	    			.filter(line -> {
	    				String tline = line.trim();
	    				// skip empty lines, comments, problem definitions, SATLIB % and 0 lines
	    				return tline.length() > 0 && !tline.startsWith("c") && !tline.startsWith("p") && !tline.startsWith("%") && !tline.equals("0");
	    			})
					.map(line -> {
						// Each clause is a sequence of distinct non-null numbers between -nbvar and nbvar ending with 0 on the same line; 
						// it cannot contain the opposite literals i and -i simultaneously. 
						// Positive numbers denote the corresponding variables. 
						// Negative numbers denote the negations of the corresponding variables. 
						String[] literals = line.trim().split("\\s+");
                        int[]    clause = null;
                        int      last   = Integer.parseInt(literals[literals.length-1]);
                        if (last == 0) {
                        	// line ends with a 0
                        	clause = new int[literals.length-1];
                        }
                        else {
                        	// line does not end with a zero
                        	clause = new int[literals.length];
                        }
                        
                        for (int i = 0; i < clause.length; i++) {
                        	clause[i] = Integer.parseInt(literals[i]);
                        }
												
						return clause;
					});
	    	
	    	
	    	System.out.println("# variables        = "+numVariables);
	    	System.out.println("# clauses reported = "+numClauses+", number clauses loaded = "+clauses.count());	
	    	
	    	ISolver sat4jSolver = SolverFactory.newDefault();
	    	
	    	sat4jSolver.newVar((int)numVariables);
	    	
	    	Iterator<int[]> clauseIt = clauses.toLocalIterator();
	    	Boolean result = null;
	    	while (clauseIt.hasNext()) {
	    		int[] clause = clauseIt.next();
	    		try {	
					VecInt vClause = new VecInt(clause);				
					sat4jSolver.addClause(vClause);
				} catch (ContradictionException cex) {
					result = Boolean.FALSE;
					break;
				}
	    	}
	    	if (result == null) {
	    		try {
	    			result = sat4jSolver.isSatisfiable();
	    			if (result) {
	    				int[] model = sat4jSolver.model();
	    				StringJoiner sj = new StringJoiner(", ");
	    				for (int i = 0; i < model.length; i++) {
	    					sj.add(""+model[i]);
	    				}
	    				System.out.println("model = "+sj);
	    				System.out.println("1 isSatisfiable="+sat4jSolver.isSatisfiable(new VecInt(new int[] {1,2})));
	    			}
	    		}
	    		catch (Throwable t) {
	    			t.printStackTrace();
	    		}
	    	}
	    	
	    	System.out.println("isSatisfiable="+result);
		}
	}
}
