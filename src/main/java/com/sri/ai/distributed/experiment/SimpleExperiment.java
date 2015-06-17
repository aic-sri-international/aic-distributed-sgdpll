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

import java.util.StringJoiner;

import com.google.common.base.Stopwatch;
import com.sri.ai.distributed.sat.CNFProblem;
import com.sri.ai.distributed.sat.DistributedSATSolver;
import com.sri.ai.distributed.sat.LocalSATSolver;
import com.sri.ai.distributed.sat.SATSolver;
import com.sri.ai.distributed.sat.reader.DIMACSReader;
import com.sri.ai.distributed.sat.reader.SimplifiedDIMACSReader;

/**
 * 
 * @author oreilly
 *
 */
public class SimpleExperiment {
	
	private static final boolean USE_DISTRIBUTED_SOLVER = true;

	public static void main(String[] args) {
		String       cnfFileName  = args[0];
		DIMACSReader dimacsReader = new SimplifiedDIMACSReader(); 
		
		CNFProblem cnfProblem = dimacsReader.read(cnfFileName);
		
		cnfProblem.getClauses().cache();
		
		System.out.println("# variables        = "+cnfProblem.getNumberVariables());
		System.out.println("# clauses reported = "+cnfProblem.getNumberClauses()+", number clauses loaded = "+cnfProblem.getClauses().count());	
		
		Stopwatch sw = new Stopwatch();
		
		sw.start();
		SATSolver solver = newSolver();
		int[]     model  = solver.findModel(cnfProblem);
		sw.stop();
		
		System.out.println("Took "+sw);
		
		if (model == null) {
			System.out.println("Problem is NOT satisfiable");
		}
		else {
			StringJoiner sj = new StringJoiner(", ");
			for (int i = 0; i < model.length; i++) {
				sj.add(""+model[i]);
			}
			
			System.out.println("Problem is satisfiable, example model found:"+sj);
		}
	}
	
	private static SATSolver newSolver() {
		SATSolver result = null;
		if (USE_DISTRIBUTED_SOLVER) {
			result = new DistributedSATSolver();
		}
		else {
			result = new LocalSATSolver();
		}
		return result;
	}
}
