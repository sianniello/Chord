package node;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashSet;

public class ClientHandler implements Runnable {

	private int port;
	private HashSet<InetSocketAddress> set;

	public ClientHandler(int port, HashSet<InetSocketAddress> set) {
		this.port = port;
		this.set = set;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void run() {
		Socket client = null;
		ObjectOutputStream out = null;
		ObjectInputStream in = null;

		try {
			client = new Socket("localhost", 1099);

			out = new ObjectOutputStream(client.getOutputStream());
			in = new ObjectInputStream(client.getInputStream());

			out.writeObject(new InetSocketAddress(port));
			set = (HashSet<InetSocketAddress>) in.readObject();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}	finally {
			try {
				if(!client.isClosed())
					client.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		//contact joinserver
		System.out.println("Node[" + port + "] - Network: " + set.toString());
	}
}
