package activitystreamer.server;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import activitystreamer.util.Settings;

public class Control extends Thread {
	private static final Logger log = LogManager.getLogger();
	private static ArrayList<Connection> connections;
	private static boolean term=false;
	private static Listener listener;
	
	protected static Control control = null;
	
	public static Control getInstance() {
		if(control==null){
			control=new Control();
		} 
		return control;
	}
	
	public Control() {
		// initialize the connections array
		connections = new ArrayList<Connection>();
		
		
		// start a listener
		try {
			listener = new Listener(); //listen to any incoming connection
			initiateConnection(); //make a connection to server if remote hostName is supplied
		} catch (IOException e1) {
			log.fatal("failed to startup a listening thread: "+e1);
			System.exit(-1);
		}
		start();
	}
	
	public void initiateConnection(){
		// make a connection to another server if remote hostname is supplied
		if(Settings.getRemoteHostname()!=null){
			try {
				outgoingConnection(new Socket(Settings.getRemoteHostname(),Settings.getRemotePort()));
			} catch (IOException e) {
				log.error("failed to make connection to "+Settings.getRemoteHostname()+":"+Settings.getRemotePort()+" :"+e);
				System.exit(-1);
			}
		}
	}
	
	/*
	 * Processing incoming messages from the connection.
	 * Return true if the connection should close.
	 */
	public synchronized boolean process(Connection con,String msg){
		
		JSONParser parser = new JSONParser();
		String socketAddress = Settings.socketAddress(con.getSocket());
		//System.out.println(msg + " from " + Settings.socketAddress(con.getSocket()));
		try {

			//parse the command string
			JSONObject json = (JSONObject) parser.parse(msg);
			String command = (String)json.get("command");
			
			// invalid packet
			if(command == null) {
				JSONObject response = new JSONObject();
				response.put("command", "INVALID_MESSAGE");
				response.put("info", "the received message did not contain a command");
				con.writeMsg(response.toString());	
				return true;
			}
			
			// receive AUTHENTICATE
			else if(command.compareTo("AUTHENTICATE")==0){
				String shared_secret = Settings.getSecret();
				String temp_secret = (String)json.get("secret"); //secret 
				//Authentication success, store authenticated socketAddress(IP, portNum)
				if(shared_secret.compareTo(temp_secret)==0){
					System.out.println("secret correct");
					//add Authenticated socketAddres into list
					boolean status = Settings.addAuthenticatedServer(Settings.socketAddress(con.getSocket()));
					JSONObject response = new JSONObject();
					//if server has already been authenticated, 
					//respond with INVALID_MESSAGE and close connection
					if(!status) {
						//System.out.println("already autheticated");
						response.put("command", "INVALID_MESSAGE");
						response.put("info", "the received message did not contain a command");
						con.writeMsg(response.toString());	
						return true;
					}
				}
				else {
					//if incorrect secret, respond with Authentication Fail and close connection
					JSONObject response = new JSONObject();
					response.put("command", "AUTHENTICATION_FAIL");
					String info_message =  "the supplied secret is incorrect: "+ temp_secret;
					response.put("info", info_message);
					con.writeMsg(response.toString());
					return true;
				}
			}
			//receive INVALID_MESSAGE close connection
			else if(command.compareTo("INVALID_MESSAGE")==0) {
				System.out.println("INVALID_MESSAGE");
				return true;	
			}
			//receive  AUTHTENTICATION_FAIL, close connection
			else if(command.compareTo("AUTHTENTICATION_FAIL")==0) {
				System.out.println("AUTHTENTICATION_FAIL");
				return true;	
			}
			//SERVER_ANNOUNCE
			else if(command.compareTo("SERVER_ANNOUNCE")==0){
				//check this connection is from an authenticated server
				if(Settings.getAuthenticatedServers().contains(socketAddress)) {
					//record server_announce packet information
					String server_id = (String)json.get("id");
					long load = (long)json.get("load");
					String hostName = (String)json.get("hostname");
					long port_num =  (long)json.get("port");
					Settings.addServerAnounce(server_id, (int)load, hostName, (int)port_num);
					log.info("receive an announcement from "+ hostName + ":"+ port_num +" with load "+load);
					
					//broadcast server_announce to every connected servers
					for (int i =0; i<connections.size(); i++) {
						String socket_info = Settings.socketAddress(connections.get(i).getSocket());
						if(Settings.getAuthenticatedServers().contains(socket_info) &&
								socket_info.compareTo(socketAddress) != 0){
							connections.get(i).writeMsg(msg);
						}	
					}
					return false;
				}
				//respond with invalid_message and close connection
				else {
					JSONObject response = new JSONObject();
					response.put("command", "INVALID_MESSAGE");
					response.put("info", "the received message did not contain a command");
					con.writeMsg(response.toString());	
					return true;	
				}
			}

			//LOGIN
			else if(command.compareTo("LOGIN")==0) {
				String username = (String)json.get("username");
				String secret  = (String)json.get("secret");
				//response packet
				JSONObject res = new JSONObject(); 
				//redirect packet
				JSONObject redir = new JSONObject(); 
				Boolean redirect = false;	
				for(String id : Settings.getLoadData().keySet()) {
					//a load at least 2 client less than its own
					if(Settings.getLoad() - Settings.getLoadData().get(id) >= 2) {
						redirect = true;
						redir.put("command", "REDIRECT");
						redir.put("hostname", Settings.getHostData().get(id));
						redir.put("port", Settings.getPortData().get(id));
						break;
					}
				}
				if(redirect == true) { // redirect message
					con.writeMsg(redir.toString());
					return true;
				}
			
				if(username!=null) {
					// userName is anonymous 
					if(username != null && username.compareTo("anonymous")==0) {
						// add connect info and user into into logged list
						Settings.addLoggedUser(socketAddress, "anonymous");
						Settings.addLoad(); // add one connected client
						res.put("command", "LOGIN_SUCCESS");
						res.put("info", "logged in as user anonymous");
						con.writeMsg(res.toString());
					
						return false;
					}
					// userName is not anonymous  
					else{
						//if username is found in local storage
						if(Settings.getUserProfile().containsKey(username)){
							//secret correct 
							if(secret!=null&&
									Settings.getUserProfile().get(username).compareTo(secret)==0) {
								// add connect info and user into into logged list
								Settings.addLoggedUser(socketAddress, username);
								Settings.addLoad(); // add one connected client
								res.put("command", "LOGIN_SUCCESS");
								res.put("info", "logged in as user "+ username);
								con.writeMsg(res.toString());
							
								return false;
							}
							//incorrect secret
							else {
								res.put("command", "LOGIN_FAILED");
								res.put("info",  "attempt to login with wrong secret");
								con.writeMsg(res.toString());
								return true;
							}
						}
						//userName is not found
						else {
							res.put("command", "LOGIN_FAILED");
							res.put("info",  "attempt to login with wrong secret");
							con.writeMsg(res.toString());
							return true;
						}
					}
				}
				else {
					res.put("command", "INVALID_MESSAGE");
					res.put("info", "the received message did not contain a command");
					con.writeMsg(res.toString());	
					return true;	
					
				}
			}
			
			
			//LOGOUT
			else if(command.compareTo("LOGOUT")==0){
				
		        //remove this connection and user info from logged list
		        Settings.removeLoggedUser(socketAddress);
		        //decrement on load
		        Settings.removeLoad();
		     
				//closes the connection
		     	return true;
			}
			
			
			//ACTIVITY_MESSAGE
			else if(command.compareTo("ACTIVITY_MESSAGE")==0){
				String username = (String)json.get("username");
				String secret = (String)json.get("secret");
				String activity = (String)json.get("activity");
				JSONObject response = new JSONObject();
				boolean flag = false;
				if(username != null &&  activity != null){
					// not logged
					if(!Settings.getLoggedUsers().containsKey(socketAddress)) {
						response.put("command", "AUTHENTICATION_FAIL");
						response.put("info", "please log in first");
						con.writeMsg(response.toString());	
						return true;
					}
					// user is anonymous
					if(username.compareTo("anonymous")==0){
						log.info("Activity object: "+activity+" from client "+ username);
						flag = true;
					}
					// username and secret
					else {
						if(secret!=null) {
							// username and secret match
							if(Settings.getUserProfile().containsKey(username)&&
									Settings.getUserProfile().get(username).compareTo(secret)==0) {
								log.info("Activity object: "+activity+" from client "+ username);
								flag = true;
							}
						}
						else{
							response.put("command", "INVALID_MESSAGE");
							response.put("info", "the received message did not contain a command");
							con.writeMsg(response.toString());	
							return true;
						}
					}
					//broadcast activity to connected servers and clients
					if(flag==true) {
						JSONObject new_activity = (JSONObject) parser.parse(activity);
						new_activity.put("authenticated_user", socketAddress);
						json.put("activity", new_activity.toString());
						//broadcast server_announce to every connected servers
						for (int i =0; i<connections.size(); i++) {
							String socket_info = Settings.socketAddress(connections.get(i).getSocket());
							if(socket_info.compareTo(socketAddress) != 0){
								connections.get(i).writeMsg(json.toString());
							}	
						}	
					}
					return false;
				}
				else {
					response.put("command", "INVALID_MESSAGE");
					response.put("info", "the received message did not contain a command");
					con.writeMsg(response.toString());	
					return true;
					
				}
				
			}
			
			
			//ACTIVITY_BROADCAST
			else if(command.compareTo("ACTIVITY_BROADCAST")==0){
				
				//check this connection is from an authenticated server
				if(Settings.getAuthenticatedServers().contains(socketAddress)) {
					String activity = (String)json.get("activity");
					if(activity != null) {
						//broadcast to connected servers and clients
						for (int i =0; i<connections.size(); i++) {
							String socket_info = Settings.socketAddress(connections.get(i).getSocket());
							if( socket_info.compareTo(socketAddress) != 0){
								connections.get(i).writeMsg(msg);
							}	
						}
						//process activity
						JSONObject json_activity = (JSONObject) parser.parse(activity);
						String authenticated_user = (String)json_activity.get("authenticated_user");
						log.info("Acitivity object: "+activity+" from client: "+authenticated_user);
						return false;
					}
					else {
						JSONObject response = new JSONObject();
						response.put("command", "INVALID_MESSAGE");
						response.put("info", "the received message did not contain a command");
						con.writeMsg(response.toString());	
						return true;	
					}
				}
				//respond with invalid_message and close connection
				else {
					JSONObject response = new JSONObject();
					response.put("command", "INVALID_MESSAGE");
					response.put("info", "the received message did not contain a command");
					con.writeMsg(response.toString());	
					return true;	
				}	
			}
			
			
			//REGISTER
			else if(command.compareTo("REGISTER")==0){
				String username = (String)json.get("username");
				String secret = (String)json.get("secret");
				JSONObject response = new JSONObject();
				if(username != null && secret != null) {
					// if receiving a REGISTER message from a client that has already logged in on this connection
					if(Settings.getLoggedUsers().containsKey(socketAddress)){
						response.put("command", "INVALID_MESSAGE");
						response.put("info", "the received message did not contain a command");
						con.writeMsg(response.toString());	
						return true;							
					}
					// if userName exists in the local storage or a userName is already in lock request list
					else if(Settings.getUserProfile().containsKey(username)||Settings.getLockRequest().containsValue(username)) {
						response.put("command", "REGISTER_FAIL");
						response.put("info", username+" is already registered with the system");
						con.writeMsg(response.toString());	
						return true;
					}
					// if username not exists in the local storage
					else {
						Settings.addRequestRegister(username, secret); // record userName and secret in waiting list
						Settings.addRequestSocket(username, socketAddress);; // record userName and socket in waiting list
						Settings.addLockRequest(username); // record number of lock allowed received  
						//broadcast lock request 
						response.put("command", "LOCK_REQUEST");
						response.put("username", username);
						response.put("secret", secret); 
						for (int i =0; i<connections.size(); i++) {
							String socket_info = Settings.socketAddress(connections.get(i).getSocket());
							if(Settings.getAuthenticatedServers().contains(socket_info)){
								connections.get(i).writeMsg(response.toString());
							}	
						}
						return false;		
					}
				}
				else {
					response.put("command", "INVALID_MESSAGE");
					response.put("info", "the received message did not contain a command");
					con.writeMsg(response.toString());	
					return true;		
				}
			}
			
			
			//LOCK_REQUEST
			else if(command.compareTo("LOCK_REQUEST")==0){
				JSONObject response = new JSONObject();
				// connection from authenticated server
				if(Settings.getAuthenticatedServers().contains(socketAddress)) {
					String username = (String)json.get("username");
					String secret = (String)json.get("secret");
					if(username!=null && secret !=null) {
						// broadcast lock request packet to other servers
						for (int i =0; i<connections.size(); i++) {
							String socket_info = Settings.socketAddress(connections.get(i).getSocket());
							if(Settings.getAuthenticatedServers().contains(socket_info)&&
									socket_info.compareTo(socketAddress) != 0){
								connections.get(i).writeMsg(msg);
							}	
						}
						// userName exist in the local storage
						if(Settings.getUserProfile().containsKey(username)){
							// broadcast a lock_denied packet
							response.put("command", "LOCK_DENIED");
							response.put("username", username);
							response.put("secret", secret);
							for (int i =0; i<connections.size(); i++) {
								String socket_info = Settings.socketAddress(connections.get(i).getSocket());
								if(Settings.getAuthenticatedServers().contains(socket_info)){
									connections.get(i).writeMsg(response.toString());
								}	
							}
							return false;
						}
						// userName is not exist in the local storage			
						else {
							//register the user in the local storage
							Settings.addUser(username, secret);
							//broadcast a lock_allowed packet	
							response.put("command", "LOCK_ALLOWED");
							response.put("username", username);
							response.put("secret", secret);
							for (int i =0; i<connections.size(); i++) {
								String socket_info = Settings.socketAddress(connections.get(i).getSocket());
								if(Settings.getAuthenticatedServers().contains(socket_info)){
									connections.get(i).writeMsg(response.toString());
								}	
							}
							return false;	
						}
					}
					else {
						response.put("command", "INVALID_MESSAGE");
						response.put("info", "the received message did not contain a command");
						con.writeMsg(response.toString());	
						return true;	
					}
				}
				else {
					response.put("command", "INVALID_MESSAGE");
					response.put("info", "the received message did not contain a command");
					con.writeMsg(response.toString());	
					return true;			
				}		
			}
			
			
			//LOCK_DENIED
			else if(command.compareTo("LOCK_DENIED")==0){
				JSONObject response = new JSONObject();
				// connection from an authenticated server
				if(Settings.getAuthenticatedServers().contains(socketAddress)) {
					String username = (String)json.get("username");
					String secret = (String)json.get("secret");
					if(username!=null && secret !=null) {
						// broadcast lock_denied to other servers
						for (int i =0; i<connections.size(); i++) {
							String socket_info = Settings.socketAddress(connections.get(i).getSocket());
							if(Settings.getAuthenticatedServers().contains(socket_info)&&
									socket_info.compareTo(socketAddress) != 0){
								connections.get(i).writeMsg(msg);
							}	
						}
						//if userName and secret is in the lock request list
						if(Settings.getRequestRegisters().containsKey(username)){
							if(Settings.getRequestRegisters().get(username).compareTo(secret)==0) {
								//send REGISTER_FAILED to the socket that sent a register
								for(int k=0; k<connections.size(); k++) {
									String con_info = Settings.socketAddress(connections.get(k).getSocket());
									if(con_info.compareTo(Settings.getRequestSockets().get(username))==0) {
										//send REGISTER_FAILED to the connection
										response.put("command", "REGISTER_FAIL");
										response.put("info", username+" is already registered with the system");
										connections.get(k).writeMsg(response.toString());
										break;
									}
								}
								// remove records for the registry
								Settings.removeRequestRegister(username);
								Settings.removeRequestSocket(username);
								Settings.removeLockRequest(username);
							}
						}
						return true;
					}
					//invalid message
					else {
						response.put("command", "INVALID_MESSAGE");
						response.put("info", "the received message did not contain a command");
						con.writeMsg(response.toString());
						return true;
						
					}
				}
				//from unauthenticated server
				else {
					response.put("command", "INVALID_MESSAGE");
					response.put("info", "the received message did not contain a command");
					con.writeMsg(response.toString());
					return true;	
				}	
			}
			
			
			//LOCK_ALLOWED
			else if(command.compareTo("LOCK_ALLOWED")==0){
				JSONObject response = new JSONObject();
				// connection from an authenticated server
				if(Settings.getAuthenticatedServers().contains(socketAddress)) {
					String username = (String)json.get("username");
					String secret = (String)json.get("secret");
					if(username!=null && secret !=null) {
						// broadcast lock_allowed to other servers
						for (int i =0; i<connections.size(); i++) {
							String socket_info = Settings.socketAddress(connections.get(i).getSocket());
							if(Settings.getAuthenticatedServers().contains(socket_info)&&
									socket_info.compareTo(socketAddress) != 0){
								connections.get(i).writeMsg(msg);
							}	
						}
						//if userName and secret is in the lock request list
						if(Settings.getRequestRegisters().containsKey(username)){
							if(Settings.getRequestRegisters().get(username).compareTo(secret)==0) {
								//if receive lock allowed from all other servers
								if(Settings.getLockRequest().get(username)+1 == Settings.getLoadData().size()) {
									//send REGISTER_SUCCESS to the socket that sent a register
									for(int k=0; k<connections.size(); k++) {
										String con_info = Settings.socketAddress(connections.get(k).getSocket());
										if(con_info.compareTo(Settings.getRequestSockets().get(username))==0) {
											//send REGISTER_SUCCESS to the connection
											response.put("command", "REGISTER_SUCCESS");
											response.put("info", "register success for "+username);
											connections.get(k).writeMsg(response.toString());
											break;
										}
									}
								}
								else {
									//increment on number of lock allowed
									Settings.addLockAllowed(username);
									
								}
								//remove records for the registry
								Settings.removeRequestRegister(username);
								Settings.removeRequestSocket(username);
								Settings.removeLockRequest(username);
							}
						}
						return false;
					}
					//invalid message
					else {
						response.put("command", "INVALID_MESSAGE");
						response.put("info", "the received message did not contain a command");
						con.writeMsg(response.toString());
						return true;
					}
				}
				//from unauthenticated server
				else {
					response.put("command", "INVALID_MESSAGE");
					response.put("info", "the received message did not contain a command");
					con.writeMsg(response.toString());
					return true;	
				}
			}
			
			
			//respond a invalid_message and close connection
			else {
				JSONObject response = new JSONObject();
				response.put("command", "INVALID_MESSAGE");
				response.put("info", "the received message did not contain a command");
				con.writeMsg(response.toString());	
				return true;	
			}
			
			//Q: Broadcast a LOCK_DENIED to all other servers (between servers only) if the username is already
			//known to the server with a different secret.
			//Q: When a server receives this message, it will remove the username from its local storage only if the secret
			//matches the associated secret in its local storage.
			
			
			
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return false;
	}
	
	/*
	 * The connection has been closed by the other party.
	 */
	public synchronized void connectionClosed(Connection con){
		if(!term) connections.remove(con);
	}
	
	/*
	 * A new incoming connection has been established, and a reference is returned to it
	 */
	public synchronized Connection incomingConnection(Socket s) throws IOException{
		log.debug("incomming connection: "+Settings.socketAddress(s));
		Connection c = new Connection(s);
		connections.add(c);
		return c;
		
	}
	
	/*
	 * A new outgoing connection has been established, and a reference is returned to it
	 */
	public synchronized Connection outgoingConnection(Socket s) throws IOException{
		log.debug("outgoing connection: "+Settings.socketAddress(s));	
		Settings.addAuthenticatedServer(Settings.socketAddress(s));
		Connection c = new Connection(s);
		connections.add(c);
		return c;
		
	}
	
	@Override
	public void run(){
		log.info("using activity interval of "+Settings.getActivityInterval()+" milliseconds");
		while(!term){
			// do something with 5 second intervals in between
			// send server_announce to every other server
			try {
				for (int i =0; i<connections.size(); i++) {
					//get socket address from connections
					String socketAddress = Settings.socketAddress(connections.get(i).getSocket());
					//if connection is from an authenticated server, send server_announce packet
					if(Settings.getAuthenticatedServers().contains(socketAddress)) {
						JSONObject json = new JSONObject();
						json.put("command",  "SERVER_ANNOUNCE");
						json.put("id", Settings.getServerId());
						json.put("load", Settings.getLoad());
						json.put("hostname",Settings.getLocalHostname());
						json.put("port",Settings.getLocalPort());
						connections.get(i).writeMsg(json.toString());
						
					}
				}
				Thread.sleep(Settings.getActivityInterval());
			} catch (InterruptedException e) {
				log.info("received an interrupt, system is shutting down");
				break;
			}
			if(!term){
				log.debug("doing activity");
				term=doActivity();
			}
			
		}
		log.info("closing "+connections.size()+" connections");
		// clean up
		for(Connection connection : connections){
			connection.closeCon();
		}
		listener.setTerm(true);
	}
	
	public boolean doActivity(){
		return false;
	}
	
	public final void setTerm(boolean t){
		term=t;
	}
	
	public final ArrayList<Connection> getConnections() {
		return connections;
	}
}
