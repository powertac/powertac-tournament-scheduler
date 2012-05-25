package org.powertac.tourney.scheduling;

import java.sql.ResultSet;

public class ServerPanel {
/*Basically ServerPanel is the class where the servers call I am done*/	
	private ServerPanelLet[] SP;
	private int Nservers;
	private int index; 

	/*public loadServerPanelFromDB () {		
		
	}*/

	
	public ServerPanel(DbConnection db, int noofservers)  {		
		int i;
		SP = new ServerPanelLet[noofservers];
		for(i=0;i<noofservers;i++) {
			SP[i] = new ServerPanelLet();
		}
		Nservers = noofservers;
	}	
	
	public int  LoadServerPanelFromDB (DbConnection db) throws Exception {
		
		int i,nemptyserver=0;
		int b;
		ResultSet rs;
		String sql_select = "select ServerName, ServerNumber, IsPlaying from GameServers";
		rs = db.SetQuery(sql_select);		
		for(i=0;rs.next();i++) {
			SP[i].setServerName(rs.getString("ServerName"));
			SP[i].setServerNumber(rs.getInt("ServerNumber"));
			b = rs.getInt("IsPlaying");
			if(b==0) nemptyserver++;
			SP[i].setBusy(b==1);
			SP[i].resetIsDeployed();
		}			
		return nemptyserver;
	}

	
	public Server getAvailableServer() {
		int i,j;		
		for(i = index,j=0; j<Nservers; i=(i+1)%Nservers,j++) {
			if(!SP[i].getBusy()) {
				index = (i+1)%Nservers;
				return SP[i].getServer();
			}
		}
		index = 0;
		return null;
	}
	
	
	private int fetch(int servernumber) {
		int i;
		for(i=0;(i<SP.length) && (servernumber!=SP[i].getServerNumber());i++);
		return i;
	}
	
	public void publishComplete(int servernumber) {
		int i = fetch(servernumber);
		SP[i].resetBusy();
	}
	public void publishBusy(Server servernumber) {
		
		int i = fetch(servernumber.getServerNumber());
		SP[i].setBusy(true);
		setDeployed(servernumber);
	}
	private void setDeployed(Server servernumber) {		
		int i = fetch(servernumber.getServerNumber());
		SP[i].setIsDeployed();
	}
	
	public void publishDeployedServersToDB(DbConnection db,Server[] slist) throws Exception {
		int i,len;
		String wherestring = "",sql="update PowerTAC.GameServers set IsPlaying = 1 where ";
		for(i=0;i<slist.length;i++) {
			wherestring += "ServerNumber = "+slist[i].getServerNumber()+" OR ";			
		}
		len = wherestring.length();
		if(len>0) {
			wherestring = wherestring.substring(0,len-3);
			sql += "("+wherestring+")";
			db.SetQuery(sql,"update");
		}
	}	
	
	public Server[] getScheduledServers() {
		int i,cnt=0;
		Server[]  dservers;
		for(i = 0;i<Nservers;i++) {
			if(SP[i].getIsDeployed()) cnt++;
		}	
		dservers =  new Server[cnt];
		cnt = 0;
		for(i = 0;i<Nservers;i++) {
			if(SP[i].getIsDeployed()) {
				dservers[cnt++] = SP[i].getServer();
			}
		}		
		return dservers;
	}
	
	
}