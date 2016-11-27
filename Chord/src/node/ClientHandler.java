package node;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashSet;

public class ClientHandler {

	private int port;
	private HashSet<InetSocketAddress> set;

	public ClientHandler(int port, HashSet<InetSocketAddress> set) {
		this.port = port;
		this.set = set;
	}

	@SuppressWarnings("unchecked")
	public void joinServer() throws IOException {
		Socket client = null;
		ObjectOutputStream out = null;
		ObjectInputStream in = null;

		try {
			client = new Socket("localhost", 1099);

			out = new ObjectOutputStream(client.getOutputStream());
			in = new ObjectInputStream(client.getInputStream());

			out.writeObject(new InetSocketAddress(port));

			synchronized (this) {
				set = (HashSet<InetSocketAddress>) in.readObject();
			}
			System.out.println(set);
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}	
		System.out.println("Node[" + port + "] - Network: " + set.toString());
	}
}
