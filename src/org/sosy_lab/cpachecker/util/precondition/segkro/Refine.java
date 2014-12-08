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
package org.sosy_lab.cpachecker.util.precondition.segkro;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathIterator;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.precondition.segkro.interfaces.InterpolationWithCandidates;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;

import com.google.common.collect.Lists;

public class Refine {

  private final ExtractNewPreds enp;
  private final InterpolationWithCandidates ipc;
  private final FormulaManager mgr;
  private final FormulaManagerView mgrv;
  private BooleanFormulaManagerView bmgr;

  public Refine(ExtractNewPreds pEnp, InterpolationWithCandidates pIpc, FormulaManager pMgr, FormulaManagerView pMgrv) {
    enp = pEnp;
    ipc = pIpc;
    mgr = pMgr;
    mgrv = pMgrv;
    bmgr = mgrv.getBooleanFormulaManager();
  }

  private BooleanFormula wpFromPath(PathFormula pPath) {
    return null;
  }

  private Collection<BooleanFormula> atoms(BooleanFormula pF) {
    return mgrv.extractAtoms(pF, false, false);
  }

  private BooleanFormula foo(BooleanFormula pF, BooleanFormula pCounterF) throws SolverException, InterruptedException {
    List<BooleanFormula> p = enp.extractNewPreds(pF);
    BooleanFormula f = bmgr.and(pF, bmgr.and(p));
    return ipc.getInterpolant(f, pCounterF, p);
  }


  private BooleanFormula getPathPrecond(ARGPath pPath) {
    return null;
  }

  private List<BooleanFormula> getPrecondsAlongPath(ARGPath pPath) {
    return null;
  }

  private BooleanFormula subst(BooleanFormula pF) {
    return null;
  }

  private List<BooleanFormula> subst(List<BooleanFormula> pFormulas) {
    ArrayList<BooleanFormula> result = Lists.newArrayList();
    for (BooleanFormula f: pFormulas) {
      result.add(subst(f));
    }
    return result;
  }

  private BooleanFormula getPathStatePrecondition(ARGPath pPath, ARGState pStateInPath) {
    return null;
  }

  private List<BooleanFormula> predsFromTrace(ARGPath pPath, BooleanFormula pPrecond) throws SolverException, InterruptedException {
    List<BooleanFormula> result = Lists.newArrayList();

    // TODO: It might be possible to use this code to also derive the predicate for the first sate.

    // Get additional predicates from the states along the trace
    //    (or the WPs along the trace)...

    final PathIterator it = pPath.pathIterator();
    BooleanFormula counterStatePrecond = pPrecond; // FIXME: The paper might be wrong here... or hard to understand... (we should start with the negation)

    while (it.hasNext()) {
      it.advance();
      final CFAEdge transition = it.getIncomingEdge(); // TODO: Depends on the direction of the analysis. Check this
      final ARGState state = it.getAbstractState();

      BooleanFormula statePrecond = getPathStatePrecondition(pPath, state);
      List<BooleanFormula> p = enp.extractNewPreds(statePrecond);
      statePrecond = bmgr.and(statePrecond, bmgr.and(p));

      counterStatePrecond = computeCounterPrecondition(transition, counterStatePrecond);
      pPrecond = ipc.getInterpolant(subst(statePrecond), counterStatePrecond, subst(p));

      result.addAll(atoms(pPrecond));
    }

    return result;
  }

  private BooleanFormula computeCounterPrecondition(CFAEdge pTransition, BooleanFormula pCounterStatePrecond) {
    // TODO Auto-generated method stub
    return null;
  }

  public Collection<BooleanFormula> refine(ARGPath pTraceToViolation, ARGPath pTraceToValidTermination) throws SolverException, InterruptedException {
    // Compute the WP for both traces
    BooleanFormula pcViolation = getPathPrecond(pTraceToViolation);
    BooleanFormula pcValid = getPathPrecond(pTraceToValidTermination);

    // "Enrich" the WPs with more general predicates
    pcViolation = foo(pcViolation, pcValid);
    pcValid = foo(pcValid, pcViolation);

    // Now we have an initial set of useful predicates; add them to the corresponding list.
    List<BooleanFormula> preds = Lists.newArrayList();
    preds.addAll(atoms(pcViolation));
    preds.addAll(atoms(pcValid));

    // Get additional predicates from the states along the trace
    //    (or the WPs along the trace)...
    //
    // -- along the trace to the violating state...
    preds.addAll(predsFromTrace(pTraceToViolation, pcViolation));
    // -- along the trace to the termination state...
    preds.addAll(predsFromTrace(pTraceToValidTermination, pcValid));

    return preds;
  }

}