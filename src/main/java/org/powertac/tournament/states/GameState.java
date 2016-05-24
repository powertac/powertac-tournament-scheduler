package org.powertac.tournament.states;

import java.util.EnumSet;


public enum GameState
{
  /*
  - Boot
    Games are initially set to boot_pending.
    When the job is sent to Jenkins, the TM sets it to in_progress.
    When done the Jenkins script sets it to complete or failed, depending on
    the resulting boot file. When the TM isn't able to send the job to
    Jenkins, the game is set to failed as well.

  - Game
    When the job is sent to Jenkins, the TM sets it to game_pending.
    When the sim is ready, the sim sets the game to game_ready.
    (This is done before the game is actually started.
    That's why we delay the login of the visualizers.)
    It also sets readyTime, to give the visualizer some time to log in before
    the brokers log in. Brokers are allowed to log in when game_ready and
    readyTime + 2 minutes (so the viz is logged in).
    When all the brokers are logged in (or login timeout occurs), the sim sets
    the game to in_progress.

    When the sim stops, the Jenkins script sets the game to complete.
    game_failed occurs when the script encounters problems downloading the POM-
    or boot-file, or when RunSim has problems sending the job to jenkins.
  */

  boot_pending, boot_in_progress, boot_complete, boot_failed,
  game_pending, game_ready, game_in_progress, game_complete, game_failed;

  public static final EnumSet<GameState> hasBootstrap = EnumSet.of(
      boot_complete,
      game_pending,
      game_ready,
      game_in_progress,
      game_complete);

  public static final EnumSet<GameState> isRunning = EnumSet.of(
      game_pending,
      game_ready,
      game_in_progress);

  public static final EnumSet<GameState> freeMachine = EnumSet.of(
      boot_failed,
      boot_complete,
      game_failed,
      game_complete);

  public boolean isBooting ()
  {
    return equals(GameState.boot_in_progress);
  }

  public boolean isRunning ()
  {
    return isRunning.contains(this);
  }

  public boolean isBootPending ()
  {
    return equals(boot_pending);
  }

  public boolean isBootComplete ()
  {
    return equals(boot_complete);
  }

  public boolean hasBootstrap ()
  {
    return hasBootstrap.contains(this);
  }

  public boolean isBootFailed ()
  {
    return equals(boot_failed);
  }

  public boolean isGameFailed ()
  {
    return equals(game_failed);
  }

  public boolean isFailed ()
  {
    return isBootFailed() || isGameFailed();
  }

  public boolean isComplete ()
  {
    return equals(game_complete);
  }

  public boolean isReady ()
  {
    return equals(game_ready);
  }
}
