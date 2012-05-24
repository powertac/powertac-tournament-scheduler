package org.powertac.tourney.beans;

public class Competition
{
  private String status;
  private int qualifierRoundId;
  private int finalRoundId;

  public String getStatus ()
  {
    return status;
  }

  public void setStatus (String status)
  {
    this.status = status;
  }

  public int getQualifierRoundId ()
  {
    return qualifierRoundId;
  }

  public void setQualifierRoundId (int qualifierRoundId)
  {
    this.qualifierRoundId = qualifierRoundId;
  }

  public int getFinalRoundId ()
  {
    return finalRoundId;
  }

  public void setFinalRoundId (int finalRoundId)
  {
    this.finalRoundId = finalRoundId;
  }
}
