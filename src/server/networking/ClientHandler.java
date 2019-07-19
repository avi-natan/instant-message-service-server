package server.networking;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import server.IMSServer;
import server.protocol.IMSProtocol;

/**
 * The class that is responsible to save client specific data on the server,
 * thus maintaining client state even when the client logs out. It also manages
 * the server side of the communication with its client side counterpart (ClientConnection).
 * 
 * @author Avi
 *
 */
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
	
	private Map<String, StringBuilder> friendsChats;
	
	/**
	 * The constructor. Called from the {@link IMSServer#run} loop when a new
	 * connection to the server is accepted. It constructs the ClientHandler
	 * with the given parameters and tries to open a socket and get its input
	 * and output streams (On error it throws exception).<br>
	 * <br>
	 * After constructing, the {@link #handshake} method is called, where the client
	 * initialization completes, after receiving more initialization data from the
	 * client.<br>
	 * <br>
	 * The initialization varies depending on the IMS protocol keyword received
	 * by the handshake method ("REGISTER"/"LOGIN").
	 * 
	 * @param server - A pointer to the server instance that created this handler.
	 * @param username - The username given by the user.
	 * @param email - the email given by the user. Empty if the IMSP keywords was "LOGIN".
	 * @param password - The password given by the user.
	 * @param socket - The socket created when the server socket accepted the connection.
	 */
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
		this.friendsChats = new HashMap<>();
	}

	/**
	 * The main loop of the connection handler. The connection handler starts with sending
	 * a welcome message to its client side counterpart and then starts listening in this loop
	 * for any messages received from its client side counterpart. After parsing a message
	 * according to the {@link IMSProtocol} standard, it uses {@link #processMessage} to
	 * process the message accordingly.
	 * <br>
	 * The method runs while it is not terminated, as indicated by the terminated boolean member.
	 * 
	 */
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
				//terminated = true; TODO: figure out if it should be closed in that case
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
	
	/**
	 * Process a message received by the input stream, and sends back
	 * a reply message indicating the result of the message processing.
	 * 
	 * @param message - The message received by the input stream.
	 * @throws IOException - If an I/O error occurs.
	 */
	private void processMessage(String[] message) throws IOException {
		switch(message[0]) {
		case "ADDFRIEND":
			boolean addStatus = addFriend(message[1]);
			if(addStatus) {
				server.getClientHandler(message[1]).addFriend(this.username);
				this.friendsChats.get(message[1]).append("ADDFRIEND SUCCESS: " + message[1] + System.lineSeparator());
				String[] replyToAddingFriend = new String[3];
				replyToAddingFriend[0] = "ADDFRIEND";
				replyToAddingFriend[1] = "SUCCESS";
				replyToAddingFriend[2] = message[1];
				byte[] replyToAddingFriendBytes = IMSProtocol.messageToBytes(replyToAddingFriend);
				out.write(replyToAddingFriendBytes);
				getFriend(message[1]).friendsChats.get(this.username).append("ADDFRIEND SUCCESS: " + this.username + System.lineSeparator());
				if(!getFriend(message[1]).isTerminated()) {
					String[] replyToAddedFriend = new String[3];
					replyToAddedFriend[0] = "ADDFRIEND";
					replyToAddedFriend[1] = "SUCCESS";
					replyToAddedFriend[2] = this.username;
					byte[] replyToAddedFriendBytes = IMSProtocol.messageToBytes(replyToAddedFriend);
					getFriend(message[1]).getOutputStream().write(replyToAddedFriendBytes);
				}
			} else {
				String[] replyToAddingFriend = new String[3];
				replyToAddingFriend[0] = "ADDFRIEND";
				replyToAddingFriend[1] = "FAIL";
				replyToAddingFriend[2] = message[1];
				byte[] replyToAddingFriendBytes = IMSProtocol.messageToBytes(replyToAddingFriend);
				out.write(replyToAddingFriendBytes);
			}
			break;
		case "REMOVEFRIEND":
			boolean removeStatus = removeFriend(message[1]);
			if(removeStatus) {
				server.getClientHandler(message[1]).removeFriend(this.username);
				String[] replyToRemovingFriend = new String[3];
				replyToRemovingFriend[0] = "REMOVEFRIEND";
				replyToRemovingFriend[1] = "SUCCESS";
				replyToRemovingFriend[2] = message[1];
				byte[] replyToRemovingFriendBytes = IMSProtocol.messageToBytes(replyToRemovingFriend);
				out.write(replyToRemovingFriendBytes);
				String[] replyToRemovedFriend = new String[3];
				replyToRemovedFriend[0] = "REMOVEFRIEND";
				replyToRemovedFriend[1] = "SUCCESS";
				replyToRemovedFriend[2] = this.username;
				byte[] replyToRemovedFriendBytes = IMSProtocol.messageToBytes(replyToRemovedFriend);
				server.getClientHandler(message[1]).getOutputStream().write(replyToRemovedFriendBytes);
			} else {
				String[] replyToRemovingFriend = new String[3];
				replyToRemovingFriend[0] = "REMOVEFRIEND";
				replyToRemovingFriend[1] = "FAIL";
				replyToRemovingFriend[2] = message[1];
				byte[] replyToRemovingFriendBytes = IMSProtocol.messageToBytes(replyToRemovingFriend);
				out.write(replyToRemovingFriendBytes);
			}
			break;
		case "MESSAGE":
			System.out.println("message from " + this.username + " to " + message[1]);
			ClientHandler friend = getFriend(message[1]);
			if(friend != null) {
				this.friendsChats.get(message[1]).append(this.username + ": " + message[2] + System.lineSeparator());
				friend.friendsChats.get(this.username).append(this.username + ": " + message[2] + System.lineSeparator());
				if(!friend.isTerminated()) {
					String[] replyMesasage = new String[3];
					replyMesasage[0] = "MESSAGE";
					replyMesasage[1] = this.username;
					replyMesasage[2] = message[2];
					friend.getOutputStream().write(IMSProtocol.messageToBytes(replyMesasage));
				}
			}
			break;
		default:
			break;
		}
	}
	
	/**
	 * Adds a ClientHandler that his username is the same as the input username
	 * to this ClientHandler's friends set. The ClientHandler with the same
	 * username has to exist in the server's registered clients set in order
	 * for the operation to succeed. Also this ClientHandler must not already
	 * have a friend with the same username.
	 * 
	 * @param username - the name of the friend to be added.
	 * @return True if the adding succeeded.
	 */
	private boolean addFriend(String username) {
		if(server.hasUserName(username) && friends.add(server.getClientHandler(username))) {
			this.friendsChats.put(username, new StringBuilder());
			return true;
		}
		return false;
	}
	
	/**
	 * Removes the friend specified in the input from this ClientHandler friends,
	 * and removes the chat history saved for the said friend.
	 * 
	 * @param username - The name of the friend to be removed.
	 * @return True if the removing succeeded.
	 */
	private boolean removeFriend(String username) {
		if(friends.remove(getFriend(username))) {
			this.friendsChats.remove(username);
			return true;
		}
		return false;
	}

	/**
	 * Completes the ClientConnection initialization by 
	 * suppling it with data saved in this side of the connection.<br>
	 * If the IMS protocol keyword is "REGISTER", and the registration
	 * is successful (i.e. {@link #register} returned true), then the
	 * reply sent will be "SUCCESS". If the the IMS protocol keyword is
	 * "LOGIN", and the login is successful ({@link #login}), then the reply
	 * sent will be "SUCCESS" followed by an array of this client friends
	 * names and their chat history, all in byte array that can be converted
	 * to string array using the {@link IMSProtocol}.
	 */
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
	
	/**
	 * Registers a new user to the IMS service according to the parameters
	 * given. The received parameters are in the following format:<br>
	 * <br>
	 * REGISTER &lt;username&gt; &lt;email&gt; &lt;password&gt; <br>
	 * <br>
	 * The client handler checks if there is no existing ClientHandler with
	 * the same username or email, and if there isn't it sends a success
	 * message to its client-side counterpart, adds himself to the registered
	 * clients in the server class and finally starts his {@link #run} loop
	 * in a new thread. 
	 * 
	 * @param initParams - Input parameters for the new user.
	 */
	private void register(String[] initParams) {
		try {
			this.username = initParams[1];
			this.email = initParams[2];
			this.password = initParams[3];
			
			if(server.hasUserName(this.username) || server.hasEmail(this.email)) {
				// fail the register
				String[] reply = new String[1];
				reply[0] = "FAIL";
				byte[] replyByte = IMSProtocol.messageToBytes(reply);
				out.write(replyByte);
				in.close();
				out.close();
				socket.close();
			} else {
				// register this
				String[] reply = new String[1];
				reply[0] = "SUCCESS";
				byte[] replyByte = IMSProtocol.messageToBytes(reply);
				out.write(replyByte);
				server.addToClients(this);
				new Thread(this).start();
			}
		} catch (IOException e) {
			e.printStackTrace();
			terminated = true;
		}
	}
	
	/**
	 * Logs in an existing user according to the parameters given.
	 * The received parameters are in the following format:<br>
	 * <br>
	 * LOGIN &lt;username&gt; &lt;email&gt; &lt;password&gt; <br>
	 * <br>
	 * <b>Note</b> - The email field will be empty.<br>
	 * <br>
	 * The client handler tries to get from the server the ClientHandler that
	 * his username is the same as in the parameters given. If there is no
	 * such handler (return value was null) or its password is not the same as
	 * in the parameters, or it is not terminated, indicating that a client is
	 * already connected to the IMS, it will return a "FAIL" message to the
	 * client trying to login, then closes the connections and terminates.<br>
	 * <br>
	 * Otherwise, this handler will construct a reply message with the friends
	 * and friends chats of the logging client, in order to complete the client-side
	 * client connection initialization. the message will be in this format:<br>
	 * <br>
	 * SUCCESS &lt;friend 1&gt; &lt;friend1 chat history&gt; &lt;friend 2&gt; &lt;friend2 chat history&gt;...<br>
	 * <br>
	 * After the message was sent, this client handler will set his socket and
	 * streams to be the existing client handler's ones, and will start the
	 * existing client handler's {@link #run} loop in a new thread. 
	 * 
	 * 
	 * @param initParams - Input parameters for the registered user.
	 */
	private void login(String[] initParams) {
		try {
			ClientHandler existingClient = this.server.getClientHandler(initParams[1]);
			
			if(existingClient == null || !existingClient.getPassword().equals(initParams[3]) || !existingClient.isTerminated()) {
				// fail to login
				String[] reply = new String[1];
				reply[0] = "FAIL";
				byte[] replyByte = IMSProtocol.messageToBytes(reply);
				out.write(replyByte);
				in.close();
				out.close();
				socket.close();
			} else {
				String[] successfullLogin = new String[1 + existingClient.friends.size() * 2];
				int runner = 0;
				successfullLogin[runner++] = "SUCCESS";
				for(ClientHandler ch : existingClient.friends) {
					successfullLogin[runner++] = ch.getUsername();
					successfullLogin[runner++] = existingClient.friendsChats.get(ch.getUsername()).toString();
				}
				byte[] successfullLoginBytes = IMSProtocol.messageToBytes(successfullLogin);
				out.write(successfullLoginBytes);
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
	
	/**
	 * Gets the client handler for the requested username string.
	 *  
	 * @param username - The username for which the client handler is requested.
	 * @return The corresponding client handler or null if it's not found.
	 */
	private ClientHandler getFriend(String username) {
		for(ClientHandler ch : friends) {
			if(ch.getUsername().equals(username)) {
				return ch;
			}
		}
		return null;
	}
	
	/*
	 * Getters
	 */
	public String getUsername() { return this.username;	}
	public String getEmail() { return this.email; }
	public String getPassword() { return this.password; }
	public OutputStream getOutputStream() { return this.out; }
	public boolean isTerminated() { return this.terminated; }
	
	/*
	 * Setters
	 */
	public void setSocket(Socket s) { this.socket = s; }
	public void setInputStream(InputStream in) { this.in = in; }
	public void setOutputStream(OutputStream out) {this.out = out; };
	public void setNotTerminated() { this.terminated = false; }

}
