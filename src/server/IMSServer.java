package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

public class IMSServer extends Thread {
	
	private Set<ClientHandler> registeredClients = new HashSet<>();
	
	private ServerSocket server;
	private int port = 8877;
	
	public IMSServer(int port) {
		this.port = port;
		try {
			server = new ServerSocket(port);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public IMSServer() {
		try {
			server = new ServerSocket(port);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
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
	
	
	public void removeClient(ClientHandler c) {
		System.out.println("removing");
		registeredClients.remove(c);
	}
	
	public boolean hasUserName(String username) {
		for(ClientHandler ch : registeredClients) {
			if(ch.getUsername().equals(username)) return true;
		}
		return false;
	}

	public boolean hasEmail(String email) {
		for(ClientHandler ch : registeredClients) {
			if(ch.getEmail().equals(email)) return true;
		}
		return false;
	}
	
	public void addToClients(ClientHandler clientHandler) {
		registeredClients.add(clientHandler);
	}
	
	public ClientHandler getClientHandler(String username, String password) {
		for(ClientHandler ch : registeredClients) {
			if(ch.getUsername().equals(username) && ch.getPassword().equals(password) && ch.isTerminated()) {
				return ch;
			}
		}
		return null;
	}

	
	public static void main(String[] args) {

		System.out.println("Hello IMS Server!");
		
		IMSServer imsServer = new IMSServer();
		imsServer.start();

	}

	

}
