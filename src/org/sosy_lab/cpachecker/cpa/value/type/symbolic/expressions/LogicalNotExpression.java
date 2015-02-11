/*
 * CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions;

import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cpa.constraints.ConstraintVisitor;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.UnaryConstraint;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.SymbolicValueVisitor;

/**
 * {@link UnarySymbolicExpression} representing the 'logical not' operation.
 */
public class LogicalNotExpression extends UnarySymbolicExpression implements UnaryConstraint {

  protected LogicalNotExpression(SymbolicExpression pOperand, Type pType) {
    super (pOperand, pType);
  }

  @Override
  public <VisitorReturnT> VisitorReturnT accept(SymbolicValueVisitor<VisitorReturnT> pVisitor) {
    return pVisitor.visit(this);
  }

  @Override
  public <T> T accept(ConstraintVisitor<T> pVisitor) {
    return pVisitor.visit(this);
  }

  @Override
  public String toString() {
    return "!" + getOperand();
  }
}