package activitystreamer.util;

import java.math.BigInteger;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Settings {
	private static final Logger log = LogManager.getLogger();
	private static SecureRandom random = new SecureRandom();
	private static int localPort = 3780;
	private static String localHostname = "localhost";
	private static String remoteHostname = null;
	private static int remotePort = 3781;
	private static int activityInterval = 5000; // milliseconds
	private static String secret = null;
	private static String username = "anonymous";
	// record authenticated servers
	private static List<String> authenticatedServers = new ArrayList<String>();
	
	// server announce
	// server's id
	private static String serverId = nextSecret();
	// server's load
    private static int load = 0;
    
    // record server's id and load -- map<server's id , load>
    private static Map<String, Integer> load_data = new HashMap<String, Integer>();
    // record server's id and hostName, Port number--map<server's id , hostName>
    private static Map<String, String> host_data = new HashMap<String, String>();
    // record server's id and PortNum, Port number--map<server's id , portNum>
    private static Map<String, Integer> port_data = new HashMap<String, Integer>();
    
    
    //all registered users in the server
    private static Map<String, String> users_profile = new HashMap<String, String>();
    
    //users already logged into the server
    private static List<String> logged_users = new ArrayList<String>();
    
    
    
    /*
     * Get the id of the server
     */
    public static String getServerId() {
    	
    	return serverId;
    }
    
    /*
     * Get the load of the server
     */
    public static int getLoad() {
    	
    	return load;
    }
    
    /*
     * Get the load information for other servers
     */
    public static Map<String, Integer> getLoadData() {
    	
    	return load_data;
    }
    
    /*
     * Get the hostName information for other servers 
     */
    public static Map<String, String> getHostData(){
    	
    	return host_data;
    }
    
    /*
     * Get the Port Number information for other servers
     */
    public static Map<String, Integer> getPortData(){
    	
    	return port_data;
    }
    
    
    /*
     *  Record information from Json packet in server announce
     */
	public static void addServerAnounce(String serverId, int load, String hostName, int portNum){
		if (load_data.containsKey(serverId)){
			//update load
			load_data.put(serverId, load);
		}
		else {
			// create new record 
			load_data.put(serverId, load);
			host_data.put(serverId, hostName);
			port_data.put(serverId, portNum);		
		}
	}
    
	
	/*
	 * return the authenticated servers list
	 */
	public static List<String> getAuthenticatedServers(){
		return authenticatedServers;
	}

	/*
	 * add new server into authenticated server list
	 */
	public static boolean  addAuthenticatedServer(String socketAddress) {
		// if socketaddress already exists
		if(Settings.authenticatedServers.contains(socketAddress)) {
				return false;
		}
		else {
			Settings.authenticatedServers.add(socketAddress);
		}
		return true;
	}
	
	/*
	 * return all registered clients
	 */
	public static Map<String, String> getUserProfile(){
		
		return users_profile;
	};
	
	/*
	 * add a new registered client
	 */
    public static boolean addUser(String name, String secret){
		
    	if(users_profile.containsKey(name)) {
    		return false;
    	}
    	else {
    		users_profile.put(name, secret);
    		return true;
    	}
	};
	
	/*
	 * return all login clients
	 */
	public static List<String> getLoggedUsers(){
		
		return logged_users;
	}
	
	/*
	 * add a client into login list
	 */
	public static boolean addLoggedUsers(String username){
		if(logged_users.contains(username)) {
			return false;
		}
		else {
			logged_users.add(username);
			return true;
		}
	}
	
	
	public static int getLocalPort() {
		return localPort;
	}

	public static void setLocalPort(int localPort) {
		if(localPort<0 || localPort>65535){
			log.error("supplied port "+localPort+" is out of range, using "+getLocalPort());
		} else {
			Settings.localPort = localPort;
		}
	}
	
	public static int getRemotePort() {
		return remotePort;
	}

	public static void setRemotePort(int remotePort) {
		if(remotePort<0 || remotePort>65535){
			log.error("supplied port "+remotePort+" is out of range, using "+getRemotePort());
		} else {
			Settings.remotePort = remotePort;
		}
	}
	
	public static String getRemoteHostname() {
		return remoteHostname;
	}

	public static void setRemoteHostname(String remoteHostname) {
		Settings.remoteHostname = remoteHostname;
	}
	
	public static int getActivityInterval() {
		return activityInterval;
	}

	public static void setActivityInterval(int activityInterval) {
		Settings.activityInterval = activityInterval;
	}
	
	public static String getSecret() {
		return secret;
	}

	public static void setSecret(String s) {
		secret = s;
	}
	
	public static String getUsername() {
		return username;
	}

	public static void setUsername(String username) {
		Settings.username = username;
	}
	
	public static String getLocalHostname() {
		return localHostname;
	}

	public static void setLocalHostname(String localHostname) {
		Settings.localHostname = localHostname;
	}

	
	/*
	 * some general helper functions
	 */	
	public static String socketAddress(Socket socket){
		return socket.getInetAddress()+":"+socket.getPort();
	}

	public static String nextSecret() {
	    return new BigInteger(130, random).toString(32);
	 }



	
}
