package activitystreamer.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import activitystreamer.util.Settings;

public class ClientSkeleton extends Thread {
	private static final Logger log = LogManager.getLogger();	
	private static ClientSkeleton clientSolution;
	private TextFrame textFrame;
	private Socket clientSocket;
	private String hostname = Settings.getRemoteHostname();
	private int port = Settings.getRemotePort();
	private BufferedReader in; 
	private BufferedWriter out ;

	private boolean term = false;
	private boolean open = false;
	private boolean redirect = false;
	private String redirect_hostname;
	private long redirect_port;

	
   
	
	public static ClientSkeleton getInstance(){
		if(clientSolution==null){
			clientSolution = new ClientSkeleton();
		}
		return clientSolution;
	}
	
	public ClientSkeleton(){
			textFrame = new TextFrame();
			start();
			}
	

	@SuppressWarnings("unchecked")
	public void sendActivityObject(JSONObject activityObj) throws ParseException{
		try {
			//write into stream
			if(!term) {
				out.write(activityObj.toString()+"\n");
				out.flush();
				//process command
				String command = (String)activityObj.get("command");
				if(command.compareTo("LOGOUT")==0) {
					term = true;
				}
	
			}
		}
		catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	
	public void reconnect() {
		log.info("Reconnection to "+hostname+": "+port);
		term = false;
		initializeConnection();
		
	}
	
	public void redirect(String hostname, int port) {
		log.info("Redirect to "+hostname+": "+port);
		term = false;
		redirect = false;
		this.hostname = hostname;
		this.port = port;
		initializeConnection();
		
	}
	
	/*
	 * manually disconnect the current connection
	 */
	public void invokedisconnect() {
		if(open) {
			JSONObject activityObj = new JSONObject();
			activityObj.put("command", "disconnected");
			try {
				out.write(activityObj.toString()+"\n");
				out.flush();
				term  = true;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else {
			log.info("Connection has alreday been closed");
		}
		
		
	}

	
	/*
	 * disconnection
	 */
	public void disconnect() {
		if (open && clientSocket != null) {
			try {
				clientSocket.close();
				open=false;
				term = true;
				log.info("Connection closed successfully");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else{
			log.error("No connections yet");
		}
	}
	
	/*
	 * initialize a socket connection 
	 */
	public void initializeConnection() {
		try {
			log.info("start connection");
			//connect to (hostName, port)
			clientSocket = new Socket(hostname,port);
			open = true;
			//input stream, output stream
			in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));
			out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(), "UTF-8"));
			String received;
			JSONParser pser = new JSONParser();
			//keep reading data from input stream
			while(!term && (received=in.readLine()) != null) {
				JSONObject outputJson = (JSONObject) pser.parse(received);
				log.info("message recieved: "+received);
				textFrame.setOutputText(outputJson);
				//process the packet
				String command = (String)outputJson.get("command");
				//REDIRECT
				if(command.compareTo("REDIRECT")==0) {
					redirect_hostname = (String)outputJson.get("hostname");
					redirect_port = (long)outputJson.get("port");
					term = true;
					redirect = true;
				}
				//INVALID_MESSAGE, button to reconnect to the server
				else if(command.compareTo("INVALID_MESSAGE")==0) {
					
					term = true;
				}
				//AUTEHNTICATION_FAIL, close connection
				else if(command.compareTo("AUTEHNTICATION_FAIL")==0) {
					
					term = true;
					
				}
				//LOGIN_FAILED, button to reconnect to the server
				else if(command.compareTo("LOGIN_FAILED")==0) {
					
					term = true;
				}	
			}
			log.info("closing connection on "+hostname+": " +port);
			disconnect();
		
			
			
		} catch (UnknownHostException e) {
			
			// TODO Auto-generated catch block
			e.printStackTrace();
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			
			
		} catch (Exception e){
			
			e.printStackTrace();
		}
		

		//initialize a redirect connection 
		if(redirect == true) {
			redirect(redirect_hostname, (int)redirect_port);
		}
		
		
	}
	
	
	public void run(){
		
			initializeConnection();
			
	}

	
}
