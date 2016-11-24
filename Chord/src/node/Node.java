package node;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import com.google.common.hash.Hashing;

import randomFile.RandomFile;

/**
 * TODO Put here a description of what this class does.
 *
 * @author Stefano.
 *         Created 23 nov 2016.
 */
@SuppressWarnings("serial")
public class Node implements Runnable, Serializable{


	private Node succ, pred;
	private final static int m = 3;
	private int id;
	private int port;
	private HashSet<InetSocketAddress> set;
	private HashMap<Integer, Node> finger;
	private LinkedList<File> fileList;

	@SuppressWarnings({ "javadoc", "unqualified-field-access" })
	public Node(int port) throws IOException, ClassNotFoundException {
		id = Hashing.consistentHash(port, 3);
		set = new HashSet<>();
		this.port = port;
		joinServer();
		succ = null;
	}

	@SuppressWarnings("unchecked")
	private void joinServer() throws UnknownHostException, IOException, ClassNotFoundException {
		Socket client = null;
		ObjectOutputStream out = null;
		ObjectInputStream in = null;

		client = new Socket("localhost", 1099);	//contact joinserver
		out = new ObjectOutputStream(client.getOutputStream());
		in = new ObjectInputStream(client.getInputStream());

		out.writeObject(new InetSocketAddress(port));
		set = (HashSet<InetSocketAddress>) in.readObject();
		System.out.println("Node[" + port + "] - Network: " + set.toString());
		client.close();

	}

	/**
	 * called periodically. n asks the successor
	 * about its predecessor, verifies if n's immediate
	 * successor is consistent, and tells the successor about n
	 * 
	 * @param node
	 */
	protected void stabilize(Node node) {
		(new Runnable() {

			@Override
			public void run() {
				while(true) {
					try {
						System.out.println("Ring stabilization routine...");

						Node x = node.getSucc().getPred();
						if(x != null) {
							if((x.getId() > node.getId() && x.getId() < node.getSucc().getId()))
								node.setSucc(x);
							node.notify(node.getSucc());
						}

						Thread.sleep(10000);
					} catch (InterruptedException | IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}).run();
	}

	protected void notify(Node succ) throws UnknownHostException, IOException {

		Socket client = null;
		ObjectOutputStream out = null;

		client = new Socket("localhost", succ.getPort());

		if(client.isConnected()) {
			out = new ObjectOutputStream(client.getOutputStream());
			out.writeObject(port);	//node port
			out.writeObject(5);	//notify
			out.writeObject(this);
		}
		client.close();
	}

	private int getPort() {
		return port;
	}

	/**
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	@SuppressWarnings({ "javadoc" })
	public void join() throws IOException, ClassNotFoundException{
		Socket client = null;
		ObjectOutputStream out = null;
		ObjectInputStream in = null;

		//node tries to connect to ring
		for (InetSocketAddress isa : set) {
			if(isa.getPort() != port)
				client = new Socket("localhost", isa.getPort());
			if(client.isConnected()) break;
		}

		System.out.println("Connection estabilished with node " + client.getPort());

		out = new ObjectOutputStream(client.getOutputStream());
		in = new ObjectInputStream(client.getInputStream());

		out.writeObject(port);	//node port
		out.writeObject(2);		//request id (join)
		pred = null;
		succ = (Node) in.readObject();

		if(succ != null)
			System.out.println("Node [" + getId() + "] attached to ring." );
		else
			System.out.println("No ring found.");

		client.close();
	}

	private Node findSuccessor(int id) {
		Node n0;
		if (id > this.getId() && id <= this.getSucc().getId())
			return this.getSucc();
		else
			n0 = closestPrecedingNode(id);
		return n0.findSuccessor(id);
	}

	private Node closestPrecedingNode(int id) {
		for(int i = m; i == 1; i--)
			if(this.getFinger().get(i).getId() > this.getId() && this.getFinger().get(i).getId() < id)
				return this.getFinger().get(i);
		return this;
	}

	@SuppressWarnings("resource")
	private void addFile() throws IOException {
		File file = new RandomFile().getFile();
		int key = Hashing.consistentHash(file.hashCode(), m);

		//TODO find correct node

		Socket client = null;
		ObjectOutputStream out = null;

		client = new Socket("localhost", finger.get(key).getPort());
		out = new ObjectOutputStream(out);
		out.writeObject(this.getPort());
		out.writeObject(3);
		out.writeObject(file);
		client.close();
	}

	public void create() throws IOException {
		if(succ == null) {
			pred = null;
			succ = this;
			
			//finger table initialization
			finger = new HashMap<>();
			for(int i = 1; i <= m; i++) 
				finger.put(i, this); 
			
			fileList = new LinkedList<>();
			System.out.println("Ring created. Node[" + this.getId() + "], Finger = " + finger.toString());
			stabilize(this);
		}
		else
			System.out.println("Ring already created.");
	}

	public LinkedList<File> getFileList() {
		return fileList;
	}

	@SuppressWarnings("resource")
	@Override
	public void run() {
		try {

			ServerSocket server;
			server = new ServerSocket(port);

			Executor executor = Executors.newFixedThreadPool(10);

			while(true)
				executor.execute(new ServerHandler(server.accept(), this, m));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public HashSet<InetSocketAddress> getSet() {
		return set;
	}

	public int getId() {
		return id;
	}

	public Node getSucc() {
		return succ;
	}

	public void setSucc(Node succ) {
		this.succ = succ;
	}

	public Node getPred() {
		return pred;
	}

	public void setPred(Node pred) {
		this.pred = pred;
	}

	public static void main(String[] args) throws ClassNotFoundException, IOException {

		Node node = new Node(Integer.parseInt(args[0]));

		while(true) {
			System.out.println("What you gonna do?\n");
			System.out.println("1. Create Ring");
			System.out.println("2. Join Ring");
			System.out.println("3. Leave Ring");
			System.out.println("4. Add file");

			Scanner s = new Scanner(System.in);

			switch(s.nextInt()) {
			case 1:	
				node.create(); 
				break;
			case 2:	
				node.join(); 
				break;
			case 3:	//TODO leave(); break;
			case 4:	
				node.addFile(); 
				break;
			default: System.err.println("Wrong input");

			s.close();
			}
		}
	}

	public HashMap<Integer, Node> getFinger() {
		return finger;
	}

}
