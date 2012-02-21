/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.mcmillan.waitlist;

import static org.sosy_lab.cpachecker.util.AbstractElements.extractLocation;
import static org.sosy_lab.cpachecker.util.CFAUtils.leavingEdges;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.CachingPathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.ExtendedFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.TheoremProver;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;
import org.sosy_lab.cpachecker.util.predicates.interpolation.UninstantiatingInterpolationManager;
import org.sosy_lab.cpachecker.util.predicates.mathsat.MathsatFactory;
import org.sosy_lab.cpachecker.util.predicates.mathsat.MathsatFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.mathsat.MathsatTheoremProver;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class McMillanAlgorithmWithWaitlist implements Algorithm, StatisticsProvider {

  private final LogManager logger;

  private final ConfigurableProgramAnalysis cpa;

  private final ExtendedFormulaManager fmgr;
  private final PathFormulaManager pfmgr;
  private final TheoremProver prover;
  private final InterpolationManager<Formula> imgr;

  private final Map<Formula, Boolean> implicationCache = Maps.newHashMap();


  private final Timer expandTime = new Timer();
  private final Timer refinementTime = new Timer();
  private final Timer coverTime = new Timer();
  private final Timer closeTime = new Timer();
  private final Timer solverTime = new Timer();
  private int implicationChecks = 0;
  private int trivialImplicationChecks = 0;
  private int cachedImplicationChecks = 0;

  private class Stats implements Statistics {

    @Override
    public String getName() {
      return "McMillan's algorithm";
    }

    @Override
    public void printStatistics(PrintStream out, Result pResult, ReachedSet pReached) {
      out.println("Time for expand:                    " + expandTime);
      out.println("Time for refinement:                " + refinementTime);
      out.println("Time for close:                     " + closeTime);
      out.println("  Time for cover:                   " + coverTime);
      out.println("Time spent by solver for reasoning: " + solverTime);
      out.println();
      out.println("Number of implication checks:       " + implicationChecks);
      out.println("  trivial:                          " + trivialImplicationChecks);
      out.println("  cached:                           " + cachedImplicationChecks);
      out.println("Number of refinements:              " + refinementTime.getNumberOfIntervals());
    }
  }


  public McMillanAlgorithmWithWaitlist(Configuration config, LogManager pLogger, ConfigurableProgramAnalysis pCpa) throws InvalidConfigurationException, CPAException {
    logger = pLogger;
    cpa = pCpa;

    MathsatFormulaManager mfmgr = MathsatFactory.createFormulaManager(config, logger);
    fmgr = new ExtendedFormulaManager(mfmgr, config, logger);
    pfmgr = new CachingPathFormulaManager(new PathFormulaManagerImpl(fmgr, config, logger));
    prover = new MathsatTheoremProver(mfmgr);
    imgr = new UninstantiatingInterpolationManager(fmgr, pfmgr, prover, config, logger);

    prover.init();
  }

  public AbstractElement getInitialElement(CFANode location) {
    return new Vertex(fmgr.makeTrue(), cpa.getInitialElement(location));
  }

  public Precision getInitialPrecision(CFANode location) {
    return cpa.getInitialPrecision(location);
  }

  @Override
  public boolean run(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    unwind(pReachedSet);
    return true;
  }

  private void expand(Vertex v, ReachedSet reached) throws CPAException, InterruptedException {
    expandTime.start();
    try {
      assert v.getChildren().isEmpty() && !v.isCovered();
      reached.removeOnlyFromWaitlist(v);

      AbstractElement predecessor = v.getWrappedElement();
      Precision precision = reached.getPrecision(v);

      CFANode loc = extractLocation(v);
      for (CFAEdge edge : leavingEdges(loc)) {

        Collection<? extends AbstractElement> successors = cpa.getTransferRelation().getAbstractSuccessors(predecessor, precision, edge);
        if (successors.isEmpty()) {
          // edge not feasible
          continue;
        }
        assert successors.size() == 1;

        Vertex w = new Vertex(v, fmgr.makeTrue(), edge, Iterables.getOnlyElement(successors));
        reached.add(w, precision);
      }
    } finally {
      expandTime.stop();
    }
  }

  private List<Vertex> refine(final Vertex v, ReachedSet reached) throws CPAException, InterruptedException {
    refinementTime.start();
    try {
      assert (v.isTarget() && !v.getStateFormula().isFalse());

      logger.log(Level.FINER, "Refinement on " + v);

      // build list of path elements in bottom-to-top order and reverse
      List<Vertex> path = getPathFromRootTo(v);
      path = path.subList(1, path.size()); // skip root element, it has no formula

      // build list of formulas for edges
      List<Formula> pathFormulas = new ArrayList<Formula>(path.size());
      addPathFormulasToList(path, pathFormulas);

      CounterexampleTraceInfo<Formula> cex = imgr.buildCounterexampleTrace(pathFormulas, Collections.<ARTElement>emptySet());

      if (!cex.isSpurious()) {
        return Collections.emptyList(); // real counterexample
      }

      logger.log(Level.FINER, "Refinement successful");

      path = path.subList(0, path.size()-1); // skip last element, itp is always false there
      assert cex.getPredicatesForRefinement().size() ==  path.size();

      List<Vertex> changedElements = new ArrayList<Vertex>();

      for (Pair<Formula, Vertex> interpolationPoint : Pair.zipList(cex.getPredicatesForRefinement(), path)) {
        Formula itp = interpolationPoint.getFirst();
        Vertex w = interpolationPoint.getSecond();

        if (itp.isTrue()) {
          continue;
        }

        Formula stateFormula = w.getStateFormula();
        if (!implies(stateFormula, itp)) {
          w.setStateFormula(fmgr.makeAnd(stateFormula, itp));
          removeCoverageOf(w, reached);
          changedElements.add(w);
        }
      }

      // itp of last element is always false, set it
      if (!v.getStateFormula().isFalse()) {
        v.setStateFormula(fmgr.makeFalse());
        removeCoverageOf(v, reached);
        changedElements.add(v);
      }

      return changedElements;
    } finally {
      refinementTime.stop();
    }
  }

  /**
   * Check if a vertex v may potentially be covered by another vertex w.
   * It checks everything except their state formulas.
   */
  private boolean mayCover(Vertex v, Vertex w, Precision prec) throws CPAException {
    return (v != w)
        && !w.isCovered() // ???
        && w.isOlderThan(v)
        && !v.isAncestorOf(w)
        && cpa.getStopOperator().stop(v.getWrappedElement(), Collections.singleton(w.getWrappedElement()), prec);
  }

  private boolean cover(Vertex v, Vertex w, Precision prec, ReachedSet reached) throws CPAException {
    coverTime.start();
    try {
      assert !v.isCovered();

      if (mayCover(v, w, prec)
          && implies(v.getStateFormula(), w.getStateFormula())) {

        for (Vertex childOfV : v.getSubtree()) {
          // each child of v is now covered
          reached.removeOnlyFromWaitlist(childOfV);

          // each child of v now doesn't cover anything anymore
          removeCoverageOf(childOfV, reached);
        }
        v.setCoveredBy(w);

        return true;
      }
      return false;

    } finally {
      coverTime.stop();
    }
  }

  /**
   * Remove all covering relations from a node so that this node does not cover
   * any other node anymore.
   * Also adds any now uncovered lead nodes to the waitlist.
   */
  private static void removeCoverageOf(Vertex v, ReachedSet reached) {
    for (Vertex coveredByChildOfV : v.cleanCoverage()) {
      for (Vertex leaf : findLeafChildrenOf(coveredByChildOfV)) {
        reached.reAddToWaitlist(leaf);
      }
    }
  }

  private static Iterable<Vertex> findLeafChildrenOf(Vertex v) {
    return filterLeafs(v.getSubtree());
  }

  private static Iterable<Vertex> filterLeafs(Iterable<Vertex> vertices) {
    return Iterables.filter(vertices, new Predicate<Vertex>() {
      @Override
      public boolean apply(Vertex pInput) {
        // TODO don't count nodes at sink nodes
        return pInput.getChildren().isEmpty() && !pInput.getStateFormula().isFalse();
      }
    });
  }

  private boolean close(Vertex v, ReachedSet reached) throws CPAException {
    closeTime.start();
    try {
      if (v.isCovered()) {
        return true;
      }

      Precision prec = reached.getPrecision(v);
      for (AbstractElement ae : reached.getReached(v)) {
        Vertex w = (Vertex)ae;

        if (cover(v, w, prec, reached)) {
          return true; // v is now covered
        }
      }

      return false;

    } finally {
      closeTime.stop();
    }
  }

  private boolean dfs(Vertex v, ReachedSet reached) throws CPAException, InterruptedException {
    if (close(v, reached)) {
      return true; // no need to expand
    }

    if (v.isTarget()) {
      List<Vertex> changedElements = refine(v, reached);
      if (changedElements.isEmpty()) {
        return false; // real counterexample
      }

      // optimization: instead of closing all ancestors of v,
      // close only those that were strengthened during refine
      for (Vertex w : changedElements) {
        if (close(w, reached)) {
          break; // all further elements are covered anyway
        }
      }

      assert v.getStateFormula().isFalse();
      reached.remove(v);
      return true; // no need to expand further
    }

    expand(v, reached);
    for (Vertex w : v.getChildren()) {
      if (!w.getStateFormula().isFalse()) {
        dfs(w, reached);
      }
    }

    return true;
  }

  private void unwind(ReachedSet reached) throws CPAException, InterruptedException {

    outer:
    while (reached.hasWaitingElement()) {
      Vertex v = (Vertex)reached.popFromWaitlist();
      assert v.getChildren().isEmpty();
      assert !v.isCovered();

      // close parents of v
      List<Vertex> path = getPathFromRootTo(v);
      path = path.subList(0, path.size()-1); // skip v itself
      for (Vertex w : path) {
        if (close(w, reached)) {
          continue outer; // v is now covered
        }
      }

      if (!dfs(v, reached)) {
        logger.log(Level.INFO, "Bug found");
        break outer;
      }
    }
  }

  private void addPathFormulasToList(List<Vertex> path, List<Formula> pathFormulas) throws CPATransferException {
    PathFormula pf = pfmgr.makeEmptyPathFormula();
    for (Vertex w : path) {
      pf = pfmgr.makeAnd(pf, w.getIncomingEdge());
      pathFormulas.add(pf.getFormula());
      pf = pfmgr.makeEmptyPathFormula(pf); // reset formula, keep SSAMap
    }
  }

  private List<Vertex> getPathFromRootTo(Vertex v) {
    List<Vertex> path = new ArrayList<Vertex>();

    Vertex w = v;
    while (w.hasParent()) {
      path.add(w);
      w = w.getParent();
    }
    path.add(w); // root element

    return Lists.reverse(path);
  }

  private boolean implies(Formula a, Formula b) {
    implicationChecks++;

    if (a.isFalse() || b.isTrue()) {
      trivialImplicationChecks++;
      return true;
    }
    if (a.isTrue() || b.isFalse()) {
      // "true" implies only "true", but b is not "true"
      // "false" is implied only by "false", but a is not "false"
      trivialImplicationChecks++;
      return false;
    }
    if (a.equals(b)) {
      trivialImplicationChecks++;
      return true;
    }

    Formula f = fmgr.makeNot(fmgr.makeImplication(a, b));

    Boolean result = implicationCache.get(f);
    if (result != null) {
      cachedImplicationChecks++;
      return result;
    }

    solverTime.start();
    try {
      result = prover.isUnsat(f);
    } finally {
      solverTime.stop();
    }
    implicationCache.put(f, result);
    return result;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(new Stats());
  }
}
