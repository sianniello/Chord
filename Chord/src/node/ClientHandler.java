package node;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashSet;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

class ClientHandler implements Runnable {
	private int port;
	private HashSet<InetSocketAddress> set;
	
	public ClientHandler(int port, HashSet<InetSocketAddress> set) {
		this.port = port;
		this.set = set;
	}

	@SuppressWarnings({ "resource", "unchecked" })
	public void joinServer() throws IOException, ClassNotFoundException{
		Socket client = null;
		ObjectOutputStream out = null;
		ObjectInputStream in = null;

		client = new Socket("localhost", 1099);	//contact joinserver
		out = new ObjectOutputStream(client.getOutputStream());
		in = new ObjectInputStream(client.getInputStream());

		out.writeObject(new InetSocketAddress(port));
		set = (HashSet<InetSocketAddress>) in.readObject();
		System.out.println("Node[" + port + "] - Network: " + set.toString());

	}
	@Override
	public void run() {

		try {
			joinServer();
		} catch (ClassNotFoundException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
