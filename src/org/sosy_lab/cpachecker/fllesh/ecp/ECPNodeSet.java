package org.sosy_lab.cpachecker.fllesh.ecp;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;

public class ECPNodeSet implements ECPGuard, Iterable<CFANode> {
  
  private Set<CFANode> mCFANodes = new HashSet<CFANode>();
  
  public ECPNodeSet(Set<CFANode> pCFANodes) {
    mCFANodes.addAll(pCFANodes);
  }
  
  public ECPNodeSet(CFANode pCFANode) {
    mCFANodes.add(pCFANode);
  }
  
  /** copy constructor */
  public ECPNodeSet(ECPNodeSet pNodeSet) {
    this(pNodeSet.mCFANodes);
  }
  
  public ECPNodeSet intersect(ECPNodeSet pOther) {
    HashSet<CFANode> lIntersection = new HashSet<CFANode>();
    lIntersection.addAll(mCFANodes);
    lIntersection.retainAll(pOther.mCFANodes);
    
    return new ECPNodeSet(lIntersection);
  }
  
  public ECPNodeSet union(ECPNodeSet pOther) {
    HashSet<CFANode> lUnion = new HashSet<CFANode>();
    lUnion.addAll(mCFANodes);
    lUnion.addAll(pOther.mCFANodes);
    
    return new ECPNodeSet(lUnion);
  }
  
  @Override
  public int hashCode() {
    return mCFANodes.hashCode();
  }
  
  @Override 
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    
    if (pOther == null) {
      return false;
    }
    
    if (pOther.getClass().equals(getClass())) {
      ECPNodeSet lECPNodeSet = (ECPNodeSet)pOther;
      
      return mCFANodes.equals(lECPNodeSet.mCFANodes); 
    }
    
    return false;
  }
  
  public int size() {
    return mCFANodes.size();
  }
  
  public boolean isEmpty() {
    return mCFANodes.isEmpty();
  }
  
  public boolean contains(CFANode pNode) {
    return mCFANodes.contains(pNode);
  }
  
  @Override
  public String toString() {
    return mCFANodes.toString();
  }

  @Override
  public Iterator<CFANode> iterator() {
    return mCFANodes.iterator();
  }

  @Override
  public <T> T accept(ECPVisitor<T> pVisitor) {
    return pVisitor.visit(this);
  }
  
}
