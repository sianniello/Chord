package node;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashSet;
import java.util.Random;

public class ClientHandler implements Serializable{

	private Node node;

	public ClientHandler(Node node) {
		this.node = node;
	}

	/**
	 * This function tries to connect to a Join Server to obtain nodes address of bottomlay network
	 */
	@SuppressWarnings("unchecked")
	public void joinServer() {
		Socket client = null;
		ObjectOutputStream out = null;
		ObjectInputStream in = null;

		try {
			client = new Socket("localhost", 1099);
			out = new ObjectOutputStream(client.getOutputStream());
			in = new ObjectInputStream(client.getInputStream());

			out.writeObject(new InetSocketAddress(node.getPort()));
			node.setSet((HashSet<InetSocketAddress>) in.readObject());
			client.close();
		} catch (IOException | ClassNotFoundException e) {
			System.err.println("Connection refused.");
		}
		System.out.println("Node[" + node.getPort() + "] - Network: " + node.getSet().toString());
	}

	public void joinRequest(int port) {
		Forwarder f = new Forwarder();
		if(port == -1) {
			int size = node.getSet().size();
			int item = new Random().nextInt(size);
			int i = 0;
			for(InetSocketAddress isa : node.getSet()) {
				if(i == item)
					port = isa.getPort();	
				i+=1;
			}
		}
		Request req = new Request(port, Request.join_REQ, node);
		try {
			f.send(req);
		}catch (IOException e) {
			System.err.println("Port: " + port + " - Connection refused.");
		}
	}

	public void addFileReq(int k) {
		Forwarder f = new Forwarder();
		Request request = new Request(node.getSucc().getPort(), Request.addFile_REQ);
		try {
			f.send(request);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void addFile(Node node, File file) {
		Forwarder f = new Forwarder();
		Request req = new Request(node.getPort(), Request.addFile, file);
		try {
			f.send(req);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}