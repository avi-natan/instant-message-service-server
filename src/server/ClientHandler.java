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
	
	public void register() {
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String line;
		
		try {
			line = br.readLine();
			
			byte[] b = line.getBytes();
			if(line != null && b[0] != -56) {
				String[] initParams = splitInitParams(b);
				this.username = initParams[0];
				this.email = initParams[1];
				this.password = initParams[2];
				
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
	
	private String[] splitInitParams(byte[] initParams) {
		String[] splitted = new String[3];
		int stringNum = 0;
		StringBuilder sb = new StringBuilder();
		byte[] b = {-55};
		
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

}
