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

import cryptografy.Cryptography;

@SuppressWarnings("serial")
public class ClientHandler implements Serializable{

	private Node node;
	public ClientHandler(Node node) {
		this.node = node;
	}

	public ClientHandler() {
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

	/**
	 * node join a ring. If coupling node is invalid it tries to join with a randomly chosen node from his set
	 * @param n = coupling node
	 */
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

	/**
	 * node send a request for k-corresponding node target for file saving
	 * @param succ 
	 * @param k = hashing key of file
	 */
	public void addFileReq(Node succ, int k, Node n) {
		Forwarder f = new Forwarder();
		Request request = new Request(succ.getAddress(), Request.addFile_REQ, k, n);
		try {
			f.send(request);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * node send file to save to currect k-corresponding node
	 * @param node = target node
	 * @param file = file to save
	 * @param k = hashing key of file
	 * @param node2 
	 */
	public void addFile(Node target_node, File file, int k) {
		try {
			//new Forwarder().send(new Request(target_node.getAddress(), Request.pubKey_REQ, n));
			Forwarder f = new Forwarder();
			Request req = new Request(target_node.getAddress(), Request.addFile, k, file);
			f.send(req);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * when a file is saved in node it send a copy of file to his successor
	 * @param succ = successor of current node
	 * @param file = file to replicate
	 * @param k	= hashing key of file
	 */
	public void saveReplica(Node succ, File file, int k) {
		try {
			new Forwarder().send(new Request(succ.getAddress(), Request.replicaFile, k, file));
		} catch (IOException e) {
			System.err.println("Fail to send replica to successor.");
		}
	}

}