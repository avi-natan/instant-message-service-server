package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

public class IMSServer extends Thread {
	
	private Set<ClientHandler> clients = new HashSet<>();
	
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
				ClientHandler c = new ClientHandler(this, clientSocket); 
				clients.add(c);
				new Thread(c).start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	public void removeClient(ClientHandler c) {
		System.out.println("removing");
		clients.remove(c);
	}

	
	public static void main(String[] args) {

		System.out.println("Hello IMS Server!");
		
		IMSServer imsServer = new IMSServer();
		imsServer.start();

	}

}
