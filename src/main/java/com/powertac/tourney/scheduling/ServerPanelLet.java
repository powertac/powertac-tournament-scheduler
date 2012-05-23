package tournamentscheduler;



public class ServerPanelLet {
	private boolean isdeployed;
	private boolean busy; 
	private Server server;
	
	ServerPanelLet() {
		server = new Server();		
	}

	public void resetBusy() {
		busy = false;		
	}
	public void setBusy(boolean b) {
		busy = b;		
	}
	public void setServerName(String a) {
		server.setServerName(a);	
	}
	public String getServerName() {
		return server.getServerName();	
	}	
	public void setServerNumber(int s)	{
		server.setServerNumber(s);
	}
	public int getServerNumber()	{
		return server.getServerNumber();
	}
	public boolean getBusy() {		
		return busy;		
	}	
	public Server getServer() {
		return server;
	}
	public void setIsDeployed ()  {
		isdeployed = true;	
	}
	public void resetIsDeployed() {
		isdeployed = false;
	}
	public boolean getIsDeployed() {
		return isdeployed;		
	}
}

