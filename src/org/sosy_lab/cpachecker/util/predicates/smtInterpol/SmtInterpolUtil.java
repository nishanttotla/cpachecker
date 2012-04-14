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
package org.sosy_lab.cpachecker.util.predicates.smtInterpol;

import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.Sets;

import de.uni_freiburg.informatik.ultimate.logic.ApplicationTerm;
import de.uni_freiburg.informatik.ultimate.logic.ConstantTerm;
import de.uni_freiburg.informatik.ultimate.logic.FunctionSymbol;
import de.uni_freiburg.informatik.ultimate.logic.Rational;
import de.uni_freiburg.informatik.ultimate.logic.Term;

/** This is a Class similiar to Mathsat-NativeApi,
 *  it contains some useful functions. */
public class SmtInterpolUtil {

  static boolean log = false; // debug

  /** A Term is an Atom, iff its function is no element of {"And", "Or", "Not"}.*/
  public static boolean isAtom(Term t) {
    boolean is = !isAnd(t) && !isOr(t) && !isNot(t);
    if (log) System.out.println("   isAtom (" + is +"): " + t);
    return is;
  }

  public static boolean isVariable(Term t) {
    boolean is = !isTrue(t) && !isFalse(t)
        && (t instanceof ApplicationTerm)
        && ((ApplicationTerm) t).getParameters().length == 0;
    if (log) System.out.println("   isVariable (" + is +"): " + t);
    return is;
  }

  /** check for ConstantTerm with Number or
   * ApplicationTerm with negative Number */
  public static boolean isNumber(Term t) {
    boolean is = false;
    // ConstantTerm with Number --> "123"
    if (t instanceof ConstantTerm) {
      Object value = ((ConstantTerm) t).getValue();
      if (value instanceof Number || value instanceof Rational) {
        is = true;
      }

    } else if (t instanceof ApplicationTerm) {
      ApplicationTerm at = (ApplicationTerm) t;

      // ApplicationTerm with negative Number --> "(- 123)"
      if ("-".equals(at.getFunction().getName())
          && (at.getParameters().length == 1)
          && isNumber(at.getParameters()[0])) {
        is = true;

        // ApplicationTerm with Division --> "(/ 1 5)"
      } else if ("/".equals(at.getFunction().getName())
          && (at.getParameters().length == 2)
          && isNumber(at.getParameters()[0])
          && isNumber(at.getParameters()[1])) {
        is = true;
      }
    }

    // TODO hex or binary data, string?
    if (log)
      System.out.println("   isNumber (" + is + "): " + t);
    return is;
  }

  /** converts a term to a number,
   * currently only Double is supported. */
  public static double toNumber(Term t) {
    assert isNumber(t) : "term is not a number: " + t;

    // ConstantTerm with Number --> "123"
    if (t instanceof ConstantTerm) {
      Object value = ((ConstantTerm) t).getValue();
      if (value instanceof Number) {
        return ((Number) value).doubleValue();
      } else if (value instanceof Rational) {
        Rational rat = (Rational) value;
        return rat.numerator().divide(rat.denominator()).doubleValue();
      }

      // ApplicationTerm with negative Number --> "-123"
    } else if (t instanceof ApplicationTerm) {
      ApplicationTerm at = (ApplicationTerm) t;

      if ("-".equals(at.getFunction().getName())) {
        return - toNumber(at.getParameters()[0]);
//      } else if ("/".equals(at.getFunction().getName())) {
//        return toNumber(at.getParameters()[0]) /
//          toNumber(at.getParameters()[1]);
      }
    }

    throw new NumberFormatException("unknown format of numeric term: " + t);
  }

  public static boolean isBoolean(Term t) {
    boolean is = (t instanceof ApplicationTerm)
          && t.getTheory().getBooleanSort() == ((ApplicationTerm) t).getSort();
    if (log) System.out.println("   isBoolean (" + is +"): " + t);
    return is;
  }

  /** t1 and t2
   * @param theory */
  public static boolean isAnd(Term t) {
    boolean is = (t instanceof ApplicationTerm)
        && t.getTheory().m_And == ((ApplicationTerm) t).getFunction();
    if (log) System.out.println("   isAnd (" + is +"): " + t);
    return is;
  }

  /** t1 or t2 */
  public static boolean isOr(Term t) {
    boolean is = (t instanceof ApplicationTerm)
        && t.getTheory().m_Or == ((ApplicationTerm) t).getFunction();
    if (log) System.out.println("   isOr (" + is +"): " + t);
    return is;
  }

  /** not t */
  public static boolean isNot(Term t) {
    boolean is = (t instanceof ApplicationTerm)
        && t.getTheory().m_Not == ((ApplicationTerm) t).getFunction();
    if (log) System.out.println("   isNot (" + is +"): " + t);
    return is;
  }

  /** t1 = t2 */
  public static boolean isEqual(Term t) {
    boolean is = (t instanceof ApplicationTerm)
        && "=".equals(((ApplicationTerm) t).getFunction().getName());
    if (log) System.out.println("   isEqual (" + is +"): " + t);
    return is;
  }

  public static int getArity(Term t) {
    if (t instanceof ApplicationTerm) {
      return ((ApplicationTerm) t).getParameters().length;
    } else
      return 0;
  }

  public static Term getArg(Term t, int i) {
    if (t instanceof ApplicationTerm) {
      return ((ApplicationTerm) t).getParameters()[i];
    } else
      return null;
  }

  public static boolean isTrue(Term t) {
    boolean isTrue = t.getTheory().TRUE == t;
    if (log) System.out.println("   isTrue (" + t +"): " + isTrue);
    return isTrue;
  }

  public static boolean isFalse(Term t) {
    boolean isFalse = t.getTheory().FALSE == t;
    if (log) System.out.println("   isTrue (" + t +"): " + isFalse);
    return isFalse;
  }

  /** this function creates a new Term with the same function and new parameters. */
  public static Term replaceArgs(SmtInterpolEnvironment env, Term t, Term[] newParams) {
    if (t instanceof ApplicationTerm) {
      ApplicationTerm at = (ApplicationTerm) t;
      Term[] oldParams = at.getParameters();

      assert oldParams.length == newParams.length;
      for (int i=0; i < newParams.length; i++) {
        assert oldParams[i].getSort() == newParams[i].getSort() :
          "Cannot replace " + oldParams[i] + " with " + newParams[i] + ".";
      }

      FunctionSymbol funcSymb = at.getFunction();
      return env.term(funcSymb.getName(), newParams);
    } else {
      // ConstantTerm:            numeral, nothing to replace
      // AnnotatedTerm, LetTerm:  should not happen here
      return t;
    }
  }

  /** this function returns all variables in the terms.
   * Doubles are removed. */
  public static Term[] getVars(Iterable<Term> termList) {
    Set<Term> vars = new HashSet<Term>();
    for (Term t : termList) {
      getVars(t, vars);
    }
    return vars.toArray(new Term[0]);
  }

  private static void getVars(Term t, Set<Term> vars) {
    if (t instanceof ApplicationTerm &&
        (t != t.getTheory().TRUE) && (t != t.getTheory().FALSE)) {
      Term[] params = ((ApplicationTerm) t).getParameters();
      if (params.length == 0) { // no params --> term is variable
        vars.add(t);

      } else {
        for (Term innerTerm : params) { // recursive call
          getVars(innerTerm, vars);
        }
      }
    }
  }

  /** This function simplifies a term and returns a shorter terms.
   * It factors out common children of the term.
   * Example:   (a&b)|(a&c)  -->   a&(b|c)   */
  public static Term simplify(SmtInterpolEnvironment env, Term t) {
    if (t instanceof ApplicationTerm) {
      ApplicationTerm at = (ApplicationTerm) t;
      FunctionSymbol function = at.getFunction();

      // (a&b&c)|(a&d&e)   -->    a&((b&c)|(d&e))
      if ("or".equals(function.getName())) {
        return factorOut(at, "or", env);

        // (a|b|c)&(a|d|e)   -->    a|((b|c)&(d|e))
      } else if ("and".equals(function.getName())) {
        return factorOut(at, "and", env);

      } else {
        return t;
      }
    } else {
      return t;
    }
  }

 /** This method factors out common children of the term.
  * Example:   (a&b)|(a&c)  -->   a&(b|c)   */
  private static Term factorOut(ApplicationTerm at, String function,
      SmtInterpolEnvironment env) {
    Term[] params = at.getParameters();
    assert params.length >= 2 && ("and".equals(function) || "or".equals(function));

    Term neutral = env.term("and".equals(function) ? "true" : "false");
    String childFunction = "and".equals(function) ? "or" : "and";
    Set<Term>[] children = new Set[params.length];
    for (int i = 0; i < params.length; i++) {
      if (params[i] instanceof ApplicationTerm) {
        ApplicationTerm atChild = (ApplicationTerm) params[i];
        if (childFunction.equals(atChild.getFunction().getName())) {
          children[i] = Sets.newHashSet(atChild.getParameters());
        } else {
          return at; // if one child is no AND/OR, there is no common child
        }
      } else {
        return at; // if one child is no AND/OR, there is no common child
      }
    }
    Set<Term> commonTerms = children[0];
    for (int i = 1; i < children.length; i++) {
      commonTerms = Sets.intersection(commonTerms, children[i]);
    }
    if (commonTerms.isEmpty()) {
      return at;
    }
    Term[] newChildren = new Term[children.length];
    for (int i = 0; i < children.length; i++) {
      Set<Term> diff = Sets.difference(children[i], commonTerms);
      if (diff.size() == 0) {
        newChildren[i] = neutral;
      } else if (diff.size() == 1) {
        newChildren[i] = diff.toArray(new Term[1])[0];
      } else {
        newChildren[i] = env.term(childFunction, diff.toArray(new Term[0]));
      }
    }
    Term[] ts = commonTerms.toArray(new Term[commonTerms.size()+1]);
    ts[commonTerms.size()] = env.term(function, newChildren);
    if (ts.length == 1) {
      return ts[0];
    } else {
      assert ts.length > 1;
      return env.term(childFunction, ts);
    }
  }

  /** this function can be used to print a bigger term*/
  public static String prettyPrint(Term t) {
    StringBuilder str = new StringBuilder();
    prettyPrint(t, str, 0);
    return str.toString();
  }

  private static void prettyPrint(Term t, StringBuilder str, int n) {
    for (int i=0; i<n; i++) str.append("  ");
    if (t instanceof ApplicationTerm) {
      ApplicationTerm at = (ApplicationTerm) t;
      String function = at.getFunction().getName();
      if ("and".equals(function) || "or".equals(function)) {
        str.append("(").append(function).append("\n");
        for (Term child : at.getParameters()) {
          prettyPrint(child, str, n+1);
        }
        for (int i=0; i<n; i++) str.append("  ");
        str.append(")\n");
      } else {
        str.append(t.toStringDirect()).append("\n");
      }
    } else {
      str.append(t.toStringDirect()).append("\n");
    }
  }
}
