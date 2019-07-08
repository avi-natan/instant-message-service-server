package server.networking;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

import server.IMSServer;
import server.protocol.IMSProtocol;

public class ClientHandler implements Runnable {
	
	private IMSServer server;
	private String username;
	private String email;
	private String password;
	
	private Socket socket;
	
	private InputStream in;
	private OutputStream out;
	
	private boolean terminated;
	
	private Set<ClientHandler> friends;
	
	public ClientHandler() {}

	
	public ClientHandler(IMSServer server, String username, String email, String password, Socket socket) {
		this.server = server;
		this.username = username;
		this.email = email;
		this.password = password;
		this.socket = socket;
		try {
			this.in = this.socket.getInputStream();
			this.out = this.socket.getOutputStream();
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.terminated = false;
		this.friends = new HashSet<>();
	}

	@Override
	public void run() {
		try {
			String[] welcomeMessage = new String[2];
			welcomeMessage[0] = "WELCOME";
			welcomeMessage[1] = "Welcome " + this.username + "!";
			byte[] welcomeMessageBytes = IMSProtocol.messageToBytes(welcomeMessage);
			out.write(welcomeMessageBytes);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		while(!terminated) {
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String line;
			try {
				line = br.readLine();
				byte[] b = line.getBytes();
				if(line != null && b[0] != -56) {
					String[] message = IMSProtocol.bytesToMessage(b);
					processMessage(message);
				} else {
					terminated = true;
					byte[] bb = {-56, 10};
					out.write(bb);
				}
			} catch (IOException e) {
				e.printStackTrace();
				terminated = true;
			}
		}
		
		try {
			in.close();
			out.close();
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//server.removeClient(this);

	}
	
	private void processMessage(String[] message) throws IOException {
		switch(message[0]) {
		case "ADDFRIEND":
			boolean status = addFriend(message[1]);
			if(status) {
				server.getClientHandler(message[1]).addFriend(this.username);
				String[] replyToAddingFriend = new String[3];
				replyToAddingFriend[0] = "ADDFRIEND";
				replyToAddingFriend[1] = "SUCCESS";
				replyToAddingFriend[2] = message[1];
				byte[] replyToAddingFriendBytes = IMSProtocol.messageToBytes(replyToAddingFriend);
				out.write(replyToAddingFriendBytes);
				String[] replyToAddedFriend = new String[3];
				replyToAddedFriend[0] = "ADDFRIEND";
				replyToAddedFriend[1] = "SUCCESS";
				replyToAddedFriend[2] = this.username;
				byte[] replyToAddedFriendBytes = IMSProtocol.messageToBytes(replyToAddedFriend);
				getFriend(message[1]).getOutputStream().write(replyToAddedFriendBytes);
			} else {
				String[] replyToAddingFriend = new String[3];
				replyToAddingFriend[0] = "ADDFRIEND";
				replyToAddingFriend[1] = "FAIL";
				replyToAddingFriend[2] = message[1];
				byte[] replyToAddingFriendBytes = IMSProtocol.messageToBytes(replyToAddingFriend);
				out.write(replyToAddingFriendBytes);
			}
			break;
		default:
			break;	
		}
	}
	

	private boolean addFriend(String username) {
		if(server.hasUserName(username)) {
			friends.add(server.getClientHandler(username));
			return true;
		}
		return false;
	}


	public void handshake() {
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String line;
		
		try {
			line = br.readLine();
			
			byte[] b = line.getBytes();
			if(line != null && b[0] != -56) {
				String[] initParams = IMSProtocol.bytesToMessage(b);
				if(initParams[0].equals("REGISTER")) {
					register(initParams);
				} else {
					login(initParams);
				}
			} else {
				terminated = true;
				byte[] bb = {-56, 10};
				out.write(bb);
			}
		} catch (IOException e) {
			e.printStackTrace();
			terminated = true;
		}
	}
	
	private void register(String[] initParams) {
		try {
			this.username = initParams[1];
			this.email = initParams[2];
			this.password = initParams[3];
			
			if(server.hasUserName(this.username) || server.hasEmail(this.email)) {
				// fail the register
				out.write("FAIL\n".getBytes());
				in.close();
				out.close();
				socket.close();
			} else {
				// register this
				out.write("SUCCESS\n".getBytes());
				server.addToClients(this);
				new Thread(this).start();
			}
		} catch (IOException e) {
			e.printStackTrace();
			terminated = true;
		}
	}
	
	private void login(String[] initParams) {
		try {
			ClientHandler existingClient = this.server.getClientHandler(initParams[1]);
			
			if(existingClient == null || !existingClient.getPassword().equals(initParams[3]) || !existingClient.isTerminated()) {
				// fail to login
				out.write("FAIL\n".getBytes());
				in.close();
				out.close();
				socket.close();
			} else {
				out.write("SUCCESS\n".getBytes());
				existingClient.setSocket(this.socket);
				existingClient.setInputStream(this.in);
				existingClient.setOutputStream(this.out);
				existingClient.setNotTerminated();
				new Thread(existingClient).start();
			}
		} catch (IOException e) {
			e.printStackTrace();
			terminated = true;
		}
		
	}
	
	private ClientHandler getFriend(String username) {
		for(ClientHandler ch : friends) {
			if(ch.getUsername().equals(username)) {
				return ch;
			}
		}
		return null;
	}
	
	public String getUsername() { return this.username;	}
	public String getEmail() { return this.email; }
	public String getPassword() { return this.password; }
	public OutputStream getOutputStream() { return this.out; }
	public boolean isTerminated() { return this.terminated; }
	
	public void setSocket(Socket s) { this.socket = s; }
	public void setInputStream(InputStream in) { this.in = in; }
	public void setOutputStream(OutputStream out) {this.out = out; };
	public void setNotTerminated() { this.terminated = false; }

}
