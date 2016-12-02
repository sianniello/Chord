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
	public void joinServer() throws IOException, ClassNotFoundException {
		Socket client = null;
		ObjectOutputStream out = null;
		ObjectInputStream in = null;

		client = new Socket("localhost", 1099);	//contact joinserver
		out = new ObjectOutputStream(client.getOutputStream());
		in = new ObjectInputStream(client.getInputStream());

		out.writeObject(new InetSocketAddress(port));
		set = (HashSet<InetSocketAddress>) in.readObject();
		client.close();
		System.out.println("Node[" + port + "] - Network: " + set.toString());
	}

	public void joinNode() {
		Socket client = null;
		ObjectOutputStream out = null;
		ObjectInputStream in = null;

		for(InetSocketAddress isa : set) {
			try {
				Forwarder f = new Forwarder();
				Request request = new Request(isa.getPort(), Request.join_request);
				f.send(request);
			} catch (IOException e) {
				
			}	
		}
	}
}
