package node;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Random;

@SuppressWarnings("serial")
public class ClientHandler implements Serializable{

	private Node node;
	private InetAddress joinServer;

	public ClientHandler(Node node) {
		this.node = node;
	}

	/**
	 * This function tries to connect to a Join Server to obtain nodes address of bottomlay network
	 */
	@SuppressWarnings("unchecked")
	public void joinServer(InetSocketAddress joinServer) {
		Socket client = null;
		ObjectOutputStream out = null;
		ObjectInputStream in = null;
		String join_server;
		int join_server_port;
		
		if(joinServer == null) {
			join_server = "localhost";
			join_server_port = 1099;
		}
		else {
			join_server = joinServer.getHostName();
			join_server_port = joinServer.getPort();
		}

		try {
			client = new Socket(join_server, join_server_port);
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

	public void joinRequest(int n) {
		InetSocketAddress address = null;
		if(n > 0 && n < node.getSet().size()) 
			address = node.getSet().iterator().next();
		else
		{
			int size = node.getSet().size();
			int item = new Random().nextInt(size);
			int i = 0;
			for(InetSocketAddress isa : node.getSet()) {
				if(i == item)
					address = isa;
				i+=1;
			}
		}

		Forwarder f = new Forwarder();
		Request req = new Request(address, Request.join_REQ, node);
		try {
			f.send(req);
		}catch (IOException e) {
			System.err.println(address + " - Connection refused.");
		}
	}

	public void addFileReq(int k) {
		Forwarder f = new Forwarder();
		Request request = new Request(node.getSucc().getAddress(), Request.addFile_REQ, k, node);
		try {
			f.send(request);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void addFile(Node node, File file, int k) {
		Forwarder f = new Forwarder();
		Request req = new Request(node.getAddress(), Request.addFile, k, file);
		try {
			f.send(req);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}