package node;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Random;

import com.google.common.hash.Hashing;

import randomFile.RandomFile;

class ServerHandler implements Runnable {

	private ObjectOutputStream out = null;
	private ObjectInputStream in = null;
	private Node n;
	private int m;
	private Hashtable<Integer, Node> ring;

	public ServerHandler(Socket client, Node n, int m, Hashtable<Integer, Node> ring) throws IOException {
		out = new ObjectOutputStream(client.getOutputStream());
		in = new ObjectInputStream(client.getInputStream());
		this.n = n;
		this.m = m;
		this.ring = ring;
	}

	public ServerHandler(Node n) throws IOException {
		this.n = n;
	}

	@Override
	public void run() {
		try {
			Request request = (Request) in.readObject();
			System.out.println("Node[" + n.getId() + "]: new request");
			
			switch(request.getRequest()) {
			case Request.add_file:
				int k = request.getK();
				Node n = find_successor(k);
				break;
			case Request.notify:
				if(ring.contains(request.getNode()))
					synchronized (this) {
						ring.remove(request.getNode());
						ring.add(request.getNode());
					}
				break;
			case Request.find_filek:
				System.out.println("Node[" + n.getId() + "]: Find k.");
				findSuccessor(request);
				break;
			default:
				break;
			}
		} catch (IOException | ClassNotFoundException e) {
			System.err.println("Connection lost!" + n.toString());
			e.printStackTrace();
		} 
	}

	public Node findSuccessor(int id) {
		ring.
			for(Node n : ring) 
				if((n.getSucc().getId() - n.getId() + m)%m > 
				(n.getSucc().getId() - id + m)%m)
					return n.getSucc();
		}
	
	public void addFile() {
		File file;
		Socket client = null;
		ObjectOutputStream out = null;
		ObjectInputStream in = null;
		Node k = null;
		try {
			client = new Socket("localhost", this.getPort());
			out = new ObjectOutputStream(client.getOutputStream());
			in = new ObjectInputStream(client.getInputStream());

			out.writeObject(new Request(find_successor, this));
			k = (Node) in.readObject();
			client.close();
			file = new RandomFile().getFile();

			client = new Socket("localhost", k.getPort());
			out = new ObjectOutputStream(client.getOutputStream());
			out.writeObject(new Request(add_file, file));
			out.close();
			client.close();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

}
