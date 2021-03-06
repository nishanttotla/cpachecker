/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.ufCheckingProver;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.solver.AssignableTerm.Function;
import org.sosy_lab.solver.Model;
import org.sosy_lab.solver.SolverException;
import org.sosy_lab.solver.api.BasicProverEnvironment;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.BooleanFormulaManager;

import com.google.common.collect.Iterables;

public class UFCheckingBasicProverEnvironment<T> implements BasicProverEnvironment<T> {

  private final BasicProverEnvironment<T> delegate;
  private final LogManager logger;
  private final BooleanFormulaManager bfmgr;
  private final FunctionApplicationManager faMgr;

  private final UFCheckingProverOptions options;

  // We count the number of pushed constraints,
  // because we keep constraints, until the last pushed formula is popped.
  private int pushedConstraintsSinceLastPush = 0;
  private final List<Integer> pushedConstraints = new ArrayList<>();


  @Options(prefix="cpa.predicate.solver.ufCheckingProver")
  public static class UFCheckingProverOptions {

    /**
     * For some UFs we can compute the correct result for the given parameters,
     * but then the solver chooses new parameters and we have to compute a new result. Again, and again.
     * Example: we try to solve "a=2 & UF_multiply(a,b)=5" and try b=[1,2,3,...].
     * Thus we abort after some iterations and ignore the invalid result of the UF.
     * This procedure should be sound.
     */
    @Option(description = "How often should we try to get a better evaluation?")
    private int maxIterationNum = 5;

    @Option(description = "C99 only defines the overflow of unsigned integer type.")
    private boolean isSignedOverflowSafe = true;

    public UFCheckingProverOptions(Configuration config) throws InvalidConfigurationException {
      config.inject(this);
    }

    boolean isSignedOverflowSafe() {
      return isSignedOverflowSafe;
    }
  }

  public UFCheckingBasicProverEnvironment(LogManager pLogger, BasicProverEnvironment<T> bpe,
      FormulaManagerView pFmgr, UFCheckingProverOptions options) {
    this.delegate = bpe;
    this.logger = pLogger;
    this.bfmgr = pFmgr.getBooleanFormulaManager();
    this.faMgr = new FunctionApplicationManager(pFmgr, pLogger, options);
    this.options = options;
  }

  @Override
  public T push(BooleanFormula f) {

    pushedConstraints.add(pushedConstraintsSinceLastPush);
    pushedConstraintsSinceLastPush = 0;

    return delegate.push(f);
  }

  @Override
  public void pop() {

    // first pop constraints
    for (int i = 0; i < pushedConstraintsSinceLastPush; i++) {
      delegate.pop();
    }

    // reset counter to last entry
    pushedConstraintsSinceLastPush = pushedConstraints.remove(pushedConstraints.size() - 1);

    // then pop the basic formula
    delegate.pop();
  }

  @Override
  public boolean isUnsat() throws SolverException, InterruptedException {
    boolean unsat = delegate.isUnsat();
    int additionalLevels = 0;
    while (!unsat) {

      // next line only succeeds if the solver supports the generation of a model.
      // TODO enable by default for MathSat?

      final Model model = getModel();
      final List<BooleanFormula> constraints = new ArrayList<>();
      for (Function uf : Iterables.filter(model.keySet(), Function.class)) {
        final Object value = model.get(uf);
        final BooleanFormula newAssignment = faMgr.evaluate(uf, value);

        if (!bfmgr.isTrue(newAssignment)) {
          constraints.add(newAssignment);
        }
      }

      if (constraints.isEmpty()) {
        logger.log(Level.FINE, "no UFs to improve");
        break;
      }

      if (additionalLevels > options.maxIterationNum) {
        logger.log(Level.INFO, "aborting further sat-checks with UF-checking");
        break;
      }

      // push the new constraints and re-check for satisfiability

      additionalLevels++;
      push(bfmgr.and(constraints));
      unsat = delegate.isUnsat();
    }

    pushedConstraintsSinceLastPush += additionalLevels;

    return unsat;
  }

  @Override
  public Model getModel() throws SolverException {
    return delegate.getModel();
  }

  @Override
  public void close() {
    delegate.close();
  }
}
