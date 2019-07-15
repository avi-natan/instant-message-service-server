package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

import server.networking.ClientHandler;

/**
 * The Instant Message Service (IMS) Server class.
 * 
 * Extends Thread by having a run method, where it's main loop runs.
 * Inside this loop the server listens for new connections, and for
 * every new accepted connection it either creates a new or uses an
 * already created {@link ClientHandler}, that manages server side
 * communication for the client that the connection originated from.
 * 
 * Also provides handful of functions to be used by ClientHandlers
 * to manage the state of registered clients.
 * 
 * @author Avi
 *
 */
public class IMSServer extends Thread {
	
	private Set<ClientHandler> registeredClients = new HashSet<>();
	
	private ServerSocket server;
	private int port = 8877;
	
	/**
	 * Constructs this server with the given port as the parameter,
	 * the server will listen to the given port.
	 * 
	 * @param port - The port that the server will listen on.
	 */
	public IMSServer(int port) {
		this.port = port;
		try {
			server = new ServerSocket(port);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Constructs this server with no port as parameter. The server
	 * will listen on port 8877.
	 */
	public IMSServer() {
		try {
			server = new ServerSocket(port);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Main loop.
	 * 
	 * The server listens to incoming connections and for every
	 * connection it constructs a {@link ClientHandler} that tries
	 * to establish more permanent connection using the method
	 * {@link ClientHandler#handshake()}.
	 * 
	 */
	@Override
	public void run() {
		Socket clientSocket;
		try {
			while((clientSocket = server.accept()) != null) {
				System.out.println("new connection!");
				ClientHandler c = new ClientHandler(this, "", "", "", clientSocket);
				c.handshake();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Removes a registered {@link ClientHandler} from the registered clients database.
	 * 
	 * @param c - The client to remove.
	 * 
	 * <dt>Precondition: <dd> The client c exists in the registered clients.
	 */
	public void removeClient(ClientHandler c) {
		System.out.println("removing");
		registeredClients.remove(c);
	}
	
	/**
	 * Checks to see if the server has a registered {@link ClientHandler} with the
	 * same username as the given username.
	 * 
	 * @param username - The given username.
	 * @return True if the server contains such username.
	 */
	public boolean hasUserName(String username) {
		for(ClientHandler ch : registeredClients) {
			if(ch.getUsername().equals(username)) return true;
		}
		return false;
	}

	/**
	 * Checks to see if the server has a registered {@link ClientHandler} with the
	 * same email address as the given email.
	 * 
	 * @param email - The given email.
	 * @return True if the server contains such email.
	 */
	public boolean hasEmail(String email) {
		for(ClientHandler ch : registeredClients) {
			if(ch.getEmail().equals(email)) return true;
		}
		return false;
	}
	
	/**
	 * Registers a new {@link ClientHandler} to this server,
	 * namely adds it to the registered client handlers.
	 * 
	 * @param clientHandler - the client handler to add.
	 * 
	 * <dt>Precondition: <dd> There are no client handlers with the same
	 * username or email address as the one to be registered.
	 */
	public void addToClients(ClientHandler clientHandler) {
		registeredClients.add(clientHandler);
	}
	
	/**
	 * Returns the registered {@link ClientHandler} that has the same
	 * username, or null if there is no such ClientHandler.
	 * 
	 * @param username - The username that the desired ClientHandler has.
	 * @return The ClientHandler that has the desired username, or null.
	 */
	public ClientHandler getClientHandler(String username) {
		for(ClientHandler ch : registeredClients) {
			if(ch.getUsername().equals(username)) {
				return ch;
			}
		}
		return null;
	}

	/**
	 * Entry point for the server. The process begins from here.
	 * It constructs an instance of the IMS server and runs it.
	 * 
	 * @param args - Command line arguments.
	 */
	public static void main(String[] args) {

		System.out.println("Hello IMS Server!");
		
		IMSServer imsServer = new IMSServer();
		imsServer.start();

	}

	

}
