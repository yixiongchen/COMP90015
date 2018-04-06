package activitystreamer.server;

import java.io.IOException;
import java.net.InetAddress;
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
		try {
			//print out received packet
		   /*
			System.out.println(msg+" from "
					+ ""+ con.getSocket().getInetAddress().getHostName()+": "+con.getSocket().getPort());
			*/	
			//parse the command string
			JSONObject json = (JSONObject) parser.parse(msg);
			String command = (String)json.get("command");
			
			// Authenticate
			if(command.compareTo("AUTHENTICATE")==0){
				String shared_secret = Settings.getSecret();
				String temp_secret = (String)json.get("secret"); //secret 
				//Authentication success, store authenticated socketAddress(IP, portNum)
				if(shared_secret.compareTo(temp_secret)==0){
					System.out.println("secret correct");
					//add Authenticated socketAddres into list
					boolean status = Settings.addAuthenticatedServer(Settings.socketAddress(con.getSocket()));
					//if server has already been authenticated, 
					//respond with INVALID_MESSAGE and close connection
					if(!status) {
						//System.out.println("already autheticated");
						JSONObject response = new JSONObject();
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
				String socketAddress = Settings.socketAddress(con.getSocket());
				if(Settings.getAuthenticatedServers().contains(socketAddress)) {
					//record server_announce packet information
					String server_id = (String)json.get("id");
					long load = (long)json.get("load");
					String hostName = (String)json.get("hostname");
					long port_num =  (long)json.get("port");
					Settings.addServerAnounce(server_id, (int)load, hostName, (int)port_num);
					log.info("receive an announcement from "+ hostName + ":"+ port_num +" with load "+load);
					//create a broadcast_activity Json packet
					JSONObject broadcast = new JSONObject();
					broadcast.put("command", "ACTIVITY_BROADCAST");
					broadcast.put("activity", msg);
					//broadcast server_announce activity to every nearby servers
					for (int i =0; i<connections.size(); i++) {
						String socket_info = Settings.socketAddress(connections.get(i).getSocket());
						if(Settings.getAuthenticatedServers().contains(socket_info) &&
								socket_info.compareTo(socketAddress) != 0){
							connections.get(i).writeMsg(broadcast.toString());
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
				
				
			}
			
			
			//LOGOUT
			else if(command.compareTo("LOGOUT")==0){
				
			}
			
			//ACTIVITY_MESSAGE
			else if(command.compareTo("ACTIVITY_MESSAGE")==0){
				
				
			}
			
			
			//ACTIVITY_BROADCAST
			else if(command.compareTo("ACTIVITY_BROADCAST")==0){
				//check this connection is from an authenticated server
				String socketAddress = Settings.socketAddress(con.getSocket());
				if(Settings.getAuthenticatedServers().contains(socketAddress)) {
					String activity = (String)json.get("activity");
					JSONObject activity_json = (JSONObject) parser.parse(activity);
			
					String activity_command = (String)activity_json.get("command");
					//if activity is a server announcement
					if(activity_command.compareTo("SERVER_ANNOUNCE")==0) {
						//record server_announce packet information
						String server_id = (String)activity_json.get("id");
						long load = (long)activity_json.get("load");
						String hostName = (String)activity_json.get("hostname");
						long port_num =  (long)activity_json.get("port");
						Settings.addServerAnounce(server_id, (int)load, hostName, (int)port_num);
						log.info("receive a boradcast announcement from "+ hostName + ":"+ port_num +" with load "+load);
						//broadcast server announcement to nearby servers
						for (int i =0; i<connections.size(); i++) {
							String socket_info = Settings.socketAddress(connections.get(i).getSocket());
							if(Settings.getAuthenticatedServers().contains(socket_info) &&
									socket_info.compareTo(socketAddress) != 0){
								connections.get(i).writeMsg(msg);
							}	
						}
						return false;		
					}
					//other activities
				
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
				
			}
			
			
			//LOCK_REQUEST
			else if(command.compareTo("LOCK_REQUEST")==0){
				
			}
			
			
			//LOCK_DENIED
			else if(command.compareTo("LOCK_DENIED")==0){
				
			}
			
			
			//LOCK_ALLOWED
			else if(command.compareTo("LOCK_ALLOWED")==0){
				
			}
			
			
			//respond a invalid_message and close connection
			else {
				JSONObject response = new JSONObject();
				response.put("command", "INVALID_MESSAGE");
				response.put("info", "the received message did not contain a command");
				con.writeMsg(response.toString());	
				return true;	
				
			}
			
			
			
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
	    //add the destination server in outgoing connection to authenticated list
		String socketAddress = Settings.socketAddress(s);
		Settings.addAuthenticatedServer(socketAddress);
		
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
