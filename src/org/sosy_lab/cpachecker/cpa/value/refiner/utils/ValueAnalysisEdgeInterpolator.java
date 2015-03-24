
/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.value.refiner.utils;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.refiner.ValueAnalysisInterpolant;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.refiner.FeasibilityChecker;
import org.sosy_lab.cpachecker.util.refiner.GenericEdgeInterpolator;
import org.sosy_lab.cpachecker.util.refiner.StrongestPostOperator;

public class ValueAnalysisEdgeInterpolator
    extends GenericEdgeInterpolator<ValueAnalysisState, Pair<Value, Type>, ValueAnalysisInterpolant> {

  /**
   * This method acts as the constructor of the class.
   */
  public ValueAnalysisEdgeInterpolator(
      final FeasibilityChecker<ValueAnalysisState> pFeasibilityChecker,
      final StrongestPostOperator<ValueAnalysisState> pStrongestPostOperator,
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final CFA pCfa
  ) throws InvalidConfigurationException {

    super(pStrongestPostOperator,
          pFeasibilityChecker,
          ValueAnalysisInterpolantManager.getInstance(),
          new ValueAnalysisState(),
          ValueAnalysisCPA.class,
          pConfig,
          pLogger,
          pShutdownNotifier,
          pCfa);
  }
}
