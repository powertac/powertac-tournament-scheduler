/**
 * Created by IntelliJ IDEA.
 * User: govert
 * Date: 8/6/12
 * Time: 10:29 AM
 */

package org.powertac.tourney.beans;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * An Agent is an instance of a broker, competing in a single game
 *
 */

@Entity
@Table(name = "agents", catalog = "tourney", uniqueConstraints = {
    @UniqueConstraint(columnNames = "agentId")})
public class Agent {
  public static enum STATE {
    pending, in_progress, complete
  }

  public Agent ()
  {

  }
}