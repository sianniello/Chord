package node;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

class ServerHandler implements Runnable {

	private Socket client;
	private ObjectOutputStream out;
	private ObjectInputStream in;

	public ServerHandler(Socket client) throws IOException {
		this.client = client;
		out = new ObjectOutputStream(client.getOutputStream());
		in = new ObjectInputStream(client.getInputStream());
	}

	@Override
	public void run() {
		int client_port = 0;
		try {
			client_port = (Integer) in.readObject();
			System.out.println("Node " + client_port + " connected.");
			
			switch((Integer) in.readObject()) {
			case 2: 
				join(client_port);
				break;
			}
			
		} catch (IOException | ClassNotFoundException e) {
			System.err.println("Connection lost!");
		}
	}

	private void join(int client_port) {
		findSucc(client_port);
		
	}

	private void findSucc(int client_port) {
		// TODO Auto-generated method stub
		
	}

}
