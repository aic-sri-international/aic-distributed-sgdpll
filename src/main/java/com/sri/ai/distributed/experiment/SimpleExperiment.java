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
package com.sri.ai.distributed.experiment;

import java.util.Iterator;
import java.util.StringJoiner;

import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.ISolver;

import com.sri.ai.distributed.sat.reader.DIMACSReader;
import com.sri.ai.distributed.sat.reader.SimplifiedDIMACSReader;

/**
 * 
 * @author oreilly
 *
 */
public class SimpleExperiment {

	public static void main(String[] args) {
		String       cnfFileName  = args[0];
		DIMACSReader dimacsReader = new SimplifiedDIMACSReader(); 
		
		DIMACSReader.CNFProblem cnfProblem = dimacsReader.read(cnfFileName);
		
		cnfProblem.getClauses().cache();
		
		System.out.println("# variables        = "+cnfProblem.getNumberVariables());
		System.out.println("# clauses reported = "+cnfProblem.getNumberClauses()+", number clauses loaded = "+cnfProblem.getClauses().count());	
		
		ISolver sat4jSolver = SolverFactory.newDefault();
		
		sat4jSolver.newVar((int)cnfProblem.getNumberVariables());
		
		Iterator<int[]> clauseIt = cnfProblem.getClauses().toLocalIterator();
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
					System.out.println("1 isSatisfiable="+sat4jSolver.isSatisfiable(new VecInt(new int[] {-1,2})));
				}
			}
			catch (Throwable t) {
				t.printStackTrace();
			}
		}
		
		System.out.println("result="+result);
	}
}
