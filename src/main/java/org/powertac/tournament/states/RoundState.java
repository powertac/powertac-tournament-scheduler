package org.powertac.tournament.states;


public enum RoundState
{
  pending, in_progress, complete;

  public boolean isPending ()
  {
    return this.equals(pending);
  }

  public boolean isComplete ()
  {
    return this.equals(complete);
  }
}
