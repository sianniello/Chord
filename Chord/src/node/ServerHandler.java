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
	private Node n;

	public ServerHandler(Socket client, Node n) throws IOException {
		this.client = client;
		out = new ObjectOutputStream(client.getOutputStream());
		in = new ObjectInputStream(client.getInputStream());
		this.n = n;
	}

	@Override
	public void run() {
		int client_port = 0;
		try {
			client_port = (Integer) in.readObject();
			System.out.println("Node " + client_port + " connected.");

			switch((Integer) in.readObject()) {
			case 2: //join
				out.writeObject(join(Integer.hashCode(client_port)));
				break;
			}

		} catch (IOException | ClassNotFoundException e) {
			System.err.println("Connection lost!");
		}
	}

	private Node join(int id) {
		Node req_succ = n.findSucc(id);
		return req_succ;
	}

}
