package org.powertac.tournament.states;

import java.util.EnumSet;


public enum TournamentState
{
  open,         // This is the initial state. Accepts registrations.
  closed,       // No more registrations. Adjustments still allowed.
  scheduled0,   // Rounds for level 0 created, no more editing.
  completed0,   // Rounds for level 0 completed.
  scheduled1,
  completed1,
  scheduled2,
  completed2,
  scheduled3,
  completed3,
  complete;     // All the levels are done.

  public static final EnumSet<TournamentState> editingAllowed = EnumSet.of(
      open,
      closed,
      completed0,
      completed1,
      completed2);

  public static final EnumSet<TournamentState> schedulingAllowed = EnumSet.of(
      closed,
      completed0,
      completed1,
      completed2);

  public static final EnumSet<TournamentState> completingAllowed = EnumSet.of(
      scheduled0,
      scheduled1,
      scheduled2,
      scheduled3);


  public boolean editingAllowed ()
  {
    return editingAllowed.contains(this);
  }

  public boolean closingAllowed ()
  {
    return this.equals(open);
  }

  public boolean schedulingAllowed ()
  {
    return schedulingAllowed.contains(this);
  }

  public boolean isOpen ()
  {
    return this.equals(open);
  }

  public boolean isComplete ()
  {
    return this.equals(complete);
  }

  public int getCurrentLevelNr ()
  {
    if (this.compareTo(scheduled1) < 0) {
      return 0;
    }
    else if (this.compareTo(scheduled2) < 0) {
      return 1;
    }
    else if (this.compareTo(scheduled3) < 0) {
      return 2;
    }

    return 3;
  }
}