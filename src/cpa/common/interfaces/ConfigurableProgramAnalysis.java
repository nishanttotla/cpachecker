package cpa.common.interfaces;

import java.util.Collection;

import cfa.objectmodel.CFAFunctionDefinitionNode;

public interface ConfigurableProgramAnalysis
{
  public AbstractDomain getAbstractDomain();
  public Collection<Precision> getPrecisions ();
  public TransferRelation getTransferRelation ();
  public MergeOperator getMergeOperator ();
  public StopOperator getStopOperator ();
  public PrecisionAdjustment getPrecisionAdjustment ();
  public <AE extends AbstractElement> AE getInitialElement (CFAFunctionDefinitionNode node);
  public Precision getInitialPrecision (CFAFunctionDefinitionNode node);
}
