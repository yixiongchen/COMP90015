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
	
	
	// authenticated servers list
	private static List<String> authenticatedServers = new ArrayList<String>();
	// server's id
	private static String serverId = nextSecret();
	// server's load
    private static int load = 0;
    
    /*
	 * server announcement
	 */
    // record server's id and load -- map<server's id , load>
    private static Map<String, Integer> load_data = new HashMap<String, Integer>();
    // record server's id and hostName, Port number--map<server's id , hostName>
    private static Map<String, String> host_data = new HashMap<String, String>();
    // record server's id and PortNum, Port number--map<server's id , portNum>
    private static Map<String, Integer> port_data = new HashMap<String, Integer>();
    
    /*
     * client register
     */
    //registered users in the local storage(userName, secret)
    private static Map<String, String> registered_users = new HashMap<String, String>();
    
    //userName and secret request to register  (userName, secret)
    private static Map<String, String> request_registers = new HashMap<String, String>();
    //userName from connection socketAddress request to register (userName, socketAddress )
    private static Map<String, String> request_sockets = new HashMap<String, String>();
    //lock request list  (userName, num_of_lock_allowed)
    private static Map<String, Integer> lock_request = new HashMap<String, Integer>();
    
    /*
     * client login
     */
    //users already logged into the server(socketAddress, userName)
    private static Map<String, String> logged_users = new HashMap<String, String>();
    
    
    
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
     * add one connected client
     */
    public static void addLoad() {
    	
    	load = load + 1;
    }
    
    /*
     * add one connected client
     */
    public static void removeLoad() {
    	
    	if(load != 0) {
    		load = load - 1;
    	}
    
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
     *  Record information from server announce packet
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
		
		return registered_users;
	};
	
	/*
	 * add a new registered client
	 */
    public static boolean addUser(String name, String secret){
		
    	if(registered_users.containsKey(name)) {
    		return false;
    	}
    	else {
    		registered_users.put(name, secret);
    		return true;
    	}
	};
	
	/*
	 * remove a registered client
	 */
    public static void removeUser(String username){
		
    	if(registered_users.containsKey(username)) {
    		registered_users.remove(username);
    	}
    
	};
	
	/*
	 * return all pairs (userName, secret) that request to register
	 */
	public static Map<String, String> getRequestRegisters(){
		return request_registers;
	}
	
	/*
	 * add a pair (userName, secret) that request to register
	 */
	public static void addRequestRegister(String username, String secret) {
		request_registers.put(username, secret);
	}
	
	/*
	 * remove a pair (userName, secret) that request to register
	 */
	public static void removeRequestRegister(String username) {
		request_registers.remove(username);
	}
	
	/*
	 * return all pairs (userName, socketAddress) that request to register
	 */
	public static Map<String, String> getRequestSockets(){
		return request_sockets;
	}
	
	/*
	 * add a pair (userName, socketAddress) that request to register
	 */
	public static void addRequestSocket(String username, String socketAddress) {
		request_sockets.put(username, socketAddress);
	}
	
	/*
	 * remove a pair (userName, socketAddress)  in lock request
	 */
	public static void removeRequestSocket(String username) {
		
		request_sockets.remove(username);
	}
	
	
	/*
	 * return all pairs that record the number of lock allowed received  
	 */
	public static Map<String, Integer> getLockRequest(){
		
		return lock_request;
	}
	
	/*
	 * add a pair (userName, 0) in lock request 
	 */
	public static void addLockRequest(String userName) {
		
		lock_request.put(userName, 0);
		
	}
	
	/*
	 * increment the number of lock allowed with username
	 */
	public static void addLockAllowed(String userName) {
		
		int num = lock_request.get(userName);
		lock_request.put(userName, num+1);
		
	}
	
	/*
	 * remove a pair in lock request 
	 */
	public static void removeLockRequest(String username) {
		
		lock_request.remove(username);
		
	}
	
	
	/*
	 * return all logged clients
	 */
	public static Map<String, String> getLoggedUsers(){
		
		return logged_users;
	}
	
	/*
	 * add a user into logged with its socketAddress
	 */
	public static void addLoggedUser(String socketAddress, String username){
		
		logged_users.put(socketAddress, username);
			
	}
	
	/*
	 * remove a user from logged status 
	 */
	public static void removeLoggedUser(String socketAddress){
    	if(logged_users.containsKey(socketAddress)) {
    		logged_users.remove(socketAddress);
    	}
    	else {
    		log.error("can not log out a user from " + socketAddress);
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
