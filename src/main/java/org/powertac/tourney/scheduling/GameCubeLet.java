package org.powertac.tourney.scheduling;

/*5/21 This new class the scoreboard function and extends it */
public class GameCubeLet
{
  private int reqMax; /* stop configuration for a game */
  private int nofagents;
  private int gametype;
  private boolean lookahead;
  /* length will be the number of agents. */
  private int[] ActualSumArray;
  private int[] ProposedSumArray;
  private int[] basegame; /*
                           * for a three player and 5 agents it will look like
                           * 11100
                           */
  private int[] mask; /* initialized to the */

  public GameCubeLet (int n, int gtype, int rmax)
  {
    int i;
    reqMax = rmax;
    lookahead = false;
    mask = new int[n];
    ActualSumArray = new int[n];
    ProposedSumArray = new int[n];
    nofagents = n;
    gametype = gtype;
    basegame = new int[n];
    for (i = 0; i < n; i++) {
      if (i < gtype) {
        basegame[i] = 1;
      }
      else {
        basegame[i] = 0;
      }
      ActualSumArray[i] = 0;
      ProposedSumArray[i] = 0;
      mask[i] = i;
    }
  }

  /*
   * Most important module of
   * It is called so because the proposal need not be the scheduled game.
   */
  /* super inefficient function */
  public int[] sortAndGetIndices ()
  {
    int i, j, end, temp, max, maxindex;
    int[] tempmask;
    int[] rmask;
    rmask = new int[gametype];
    tempmask = new int[nofagents];
    /* make a copy of the mask */
    for (i = 0; i < nofagents; i++) {
      tempmask[i] = mask[i];
    }
    for (i = 0; i < nofagents; i++) {
      max = 0;
      maxindex = 0;
      for (j = 0; j < nofagents - i; j++) {
        if (max <= ProposedSumArray[j]) {
          max = ProposedSumArray[j];
          maxindex = j;
        }
      }/* j is now the end */
      end = j - 1;
      temp = ProposedSumArray[end];
      ProposedSumArray[end] = max;
      ProposedSumArray[maxindex] = temp;
      temp = tempmask[end];
      tempmask[end] = tempmask[maxindex];
      tempmask[maxindex] = temp;
    }
    return tempmask;
  }

  public void initializeCombination ()
  {
    System.arraycopy(ActualSumArray, 0, ProposedSumArray, 0,
                     ActualSumArray.length);
  }

  public void finalizeCombination (int[] tempmask)
  {
    // mask = tempmask ;
    lookahead = false;
    for (int i = 0; i < nofagents; i++) {
      if (basegame[i] == 1) {
        ActualSumArray[tempmask[i]] = ProposedSumArray[i];
      }
    }
    // System.arraycopy(ProposedSumArray,0,ActualSumArray,0,ProposedSumArray.length);

  }

  /* called if something happens */
  public void addGameToProposedSumArray ()
  {
    int i;

    for (i = 0; i < nofagents; i++) {
      ProposedSumArray[i] += basegame[i];
    }
    if ((ProposedSumArray[0]) > reqMax) {
      lookahead = true;
    }
  }

  public int getGameType ()
  {
    return gametype;
  }

  public boolean getLookAhead ()
  {

    return lookahead;
  }

  public void setLookAhead (boolean t)
  {
    lookahead = t;
  }

  public int getReqMax ()
  {
    return reqMax;
  }

  public int getCurrentMin ()
  {
    int i, temp;
    int gtype = -1;
    int min = ActualSumArray[0];
    for (i = 1; i < ActualSumArray.length; i++) {
      if (min > ActualSumArray[i]) {
        min = ActualSumArray[i];
      }
    }
    return min;
  }

  public int getTypeOfGame ()
  {
    return gametype;
  }

}
