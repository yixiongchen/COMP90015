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
					disconnect();
				}
				
			}
		}
		catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	
	
	public void disconnect(){
		if (clientSocket != null) {
			try {
				in.close();
				out.close();
				clientSocket.close();
				term = true;
				log.info("Connection closed successgully");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else{
			log.error("No connections yet");
		}
			
	}
	
	public void run(){
		try {
			//connect to (hostName, port)
			clientSocket = new Socket(hostname,port);
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
					String newHostname = (String)outputJson.get("hostname");
					long newPort = (long)outputJson.get("port");
					disconnect(); //close connection
					//open a new connection to redirected hostName and port
					try {
						clientSocket = new Socket(newHostname,(int)newPort);
						in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));
						out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(), "UTF-8"));
					}catch (UnknownHostException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (Exception e){
						e.printStackTrace();
					}
						
				}
				//INVALID_MESSAGE, button to reconnect to the server
				else if(command.compareTo("INVALID_MESSAGE")==0) {
					disconnect();
				}
				//AUTEHNTICATION_FAIL, close connection
				else if(command.compareTo("AUTEHNTICATION_FAIL")==0) {
					disconnect();
					
				}
				//LOGIN_FAILED, button to reconnect to the server
				else if(command.compareTo("LOGIN_FAILED")==0) {
					disconnect();
				}
				
			}
			
		} catch (UnknownHostException e) {
			
			// TODO Auto-generated catch block
			e.printStackTrace();
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			
			
		} catch (Exception e){
			e.printStackTrace();
		}
	}

	
}
