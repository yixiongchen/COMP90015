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
		String command = (String)activityObj.get("command");
		if(open) {
			try {
				//LOGOUT
				if(command.compareTo("LOGOUT")==0) {
					term = true; //close connection
				
				}
				//write into stream	
				out.write(activityObj.toString()+"\n");
				out.flush();
			
			}
			catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
		else {
			log.error("Connection has been already closed");
		}
		
		
	}
	
	
	public void reconnect() {
		log.info("Reconnection to "+hostname+": "+port);
		term = false;
		initializeConnection();
		
	}
	
	/*
	 * redirect to a connection with (hostname, port)
	 */
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
		
		//close input reading stream
		if(open) {
			try {
				clientSocket.shutdownInput();
				term = true;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
		}
		else {
			log.error("Connection has been alreday closed");
		}

	}

	
	/*
	 * close the socket connection
	 */
	public void disconnect() {
		if (open && clientSocket != null) {
			try {
				
				in.close();
				out.close();
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
			//reading from input-stream
			in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));
			//writhing into output-stream
			out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(), "UTF-8"));
			String received;
			JSONParser pser = new JSONParser();
			//reading data from input-stream
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
				//INVALID_MESSAGE,close connection
				else if(command.compareTo("INVALID_MESSAGE")==0) {
					
					term = true;
				}
				//AUTEHNTICATION_FAIL, close connection
				else if(command.compareTo("AUTEHNTICATION_FAIL")==0) {
					
					term = true;
					
				}
				//LOGIN_FAILED, close connection
				else if(command.compareTo("LOGIN_FAILED")==0) {
					
					term = true;
				}
				else {
					//do nothings
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
		

		//Initialize a redirect connection 
		if(redirect == true) {
			redirect(redirect_hostname, (int)redirect_port);
		}
		
		
	}
	
	
	public void run(){
		
			initializeConnection();
			
	}

	
}
