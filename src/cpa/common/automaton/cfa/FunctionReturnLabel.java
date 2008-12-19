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
/**
 * 
 */
package cpa.common.automaton.cfa;

import cpa.common.automaton.Label;
import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFAEdgeType;

/**
 * @author holzera
 *
 */
public class FunctionReturnLabel implements Label<CFAEdge> {
  private String mFunctionName;
  
  public FunctionReturnLabel(String pFunctionName) {
    mFunctionName = pFunctionName;
  }

  @Override
  public boolean matches(CFAEdge pEdge) {
    if (CFAEdgeType.ReturnEdge == pEdge.getEdgeType()) {
      return pEdge.getPredecessor().getFunctionName().equals(mFunctionName);
    }    

    return false;
  }
  
  @Override
  public boolean equals(Object pObject) {
    if (pObject == null) {
      return false;
    }
    
    if (!(pObject instanceof FunctionReturnLabel)) {
      return false;
    }
    
    FunctionReturnLabel lLabel = (FunctionReturnLabel)pObject;
    
    return mFunctionName.equals(lLabel.mFunctionName);
  }
  
  @Override
  public int hashCode() {
    return mFunctionName.hashCode();
  }
  
  @Override
  public String toString() {
    return "@RETURN(" + mFunctionName + ")";
  }
}
