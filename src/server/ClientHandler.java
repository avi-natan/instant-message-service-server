package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable {
	
	private IMSServer server;
	private String username;
	private String email;
	private String password;
	
	private Socket socket;
	
	private InputStream in;
	private OutputStream out;
	
	private boolean terminated;
	
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
	}

	@Override
	public void run() {
		try {
			socket.getOutputStream().write(("Welcome " + this.username + "\n").getBytes());
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
					String s = "You said: " + line;
					out.write(s.getBytes());
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
	
	public void handshake() {
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String line;
		
		try {
			line = br.readLine();
			
			byte[] b = line.getBytes();
			if(line != null && b[0] != -56) {
				String[] initParams = splitInitParams(b);
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
			ClientHandler existingClient = this.server.getClientHandler(initParams[1], initParams[3]);
			
			if(existingClient == null) {
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
	
	private String[] splitInitParams(byte[] initParams) {
		int stringsCount = 0;
		byte[] b = {-55};
		
		for(int i = 0; i < initParams.length; i++) {
			if(initParams[i] == b[0]) {
				stringsCount++;
			}
		}
		
		String[] splitted = new String[stringsCount];
		int stringNum = 0;
		StringBuilder sb = new StringBuilder();
		
		for(int i = 1; i < initParams.length; i++) {
			if(initParams[i] == b[0]) {
				splitted[stringNum++] = sb.toString();
				sb = new StringBuilder();
			} else {
				sb.append((char) initParams[i]);
			}
		}
		splitted[stringNum] = sb.toString();
		return splitted;
	}
	
	public String getUsername() { return this.username;	}
	public String getEmail() { return this.email; }
	public String getPassword() { return this.password; }
	public boolean isTerminated() { return this.terminated; }
	
	public void setSocket(Socket s) { this.socket = s; }
	public void setInputStream(InputStream in) { this.in = in; }
	public void setOutputStream(OutputStream out) {this.out = out; };
	public void setNotTerminated() { this.terminated = false; }

}
