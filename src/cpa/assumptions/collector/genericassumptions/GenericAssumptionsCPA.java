/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package cpa.assumptions.collector.genericassumptions;

import assumptions.AssumptionSymbolicFormulaManager;
import assumptions.MathsatInvariantSymbolicFormulaManager;
import cfa.objectmodel.CFAFunctionDefinitionNode;
import cpa.common.defaults.AbstractCPAFactory;
import cpa.common.defaults.MergeSepOperator;
import cpa.common.defaults.StaticPrecisionAdjustment;
import cpa.common.defaults.StopNeverOperator;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.CPAFactory;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.PrecisionAdjustment;
import cpa.common.interfaces.StopOperator;
import cpa.common.interfaces.TransferRelation;

public class GenericAssumptionsCPA implements ConfigurableProgramAnalysis {

  private static class GenericAssumptionsCPAFactory extends AbstractCPAFactory {
    
    @Override
    public ConfigurableProgramAnalysis createInstance() {
      return new GenericAssumptionsCPA();
    }
  }
  
  public static CPAFactory factory() {
    return new GenericAssumptionsCPAFactory();
  }
  
  private GenericAssumptionsDomain abstractDomain;
  private MergeOperator mergeOperator;
  private StopOperator stopOperator;
  private TransferRelation transferRelation;
  private PrecisionAdjustment precisionAdjustment;
  
  // Symbolic Formula Manager used to represent build invariant formulas
  private AssumptionSymbolicFormulaManager symbolicFormulaManager;
  
  private GenericAssumptionsCPA()
  {
    symbolicFormulaManager = MathsatInvariantSymbolicFormulaManager.getInstance();
    
    abstractDomain = new GenericAssumptionsDomain(this);
    
    mergeOperator = MergeSepOperator.getInstance();
    stopOperator = StopNeverOperator.getInstance();
    precisionAdjustment = StaticPrecisionAdjustment.getInstance();
    
    transferRelation = new GenericAssumptionsTransferRelation(this);
  }
  
  public AssumptionSymbolicFormulaManager getSymbolicFormulaManager()
  {
    return symbolicFormulaManager;
  }
  
  @Override
  public AbstractDomain getAbstractDomain() {
    return abstractDomain;
  }

  @Override
  public AbstractElement getInitialElement(CFAFunctionDefinitionNode pNode) {
    return abstractDomain.getTopElement();
  }

  @Override
  public Precision getInitialPrecision(CFAFunctionDefinitionNode pNode) {
    return null;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return mergeOperator;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return precisionAdjustment;
  }

  @Override
  public StopOperator getStopOperator() {
    return stopOperator;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return transferRelation;
  }

}
