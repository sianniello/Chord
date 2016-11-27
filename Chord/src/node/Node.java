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
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Random;
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
	private final static int m = 8;		//keys/ID space
	private static final int p = 3;		//finger table's entries
	private int id;
	private int port;
	private HashSet<InetSocketAddress> set;		//Bottomlay network's addresses
	private Hashtable<Integer, Node> finger;
	private Hashtable<Integer, File> fileList;	

	@SuppressWarnings({ "javadoc", "unqualified-field-access" })
	public Node(int port) throws IOException, ClassNotFoundException {
		id = Hashing.consistentHash(port, m);
		set = new HashSet<>();
		this.port = port;
		joinServer();
		succ = null;
		fileList = new Hashtable<>();
	}

	public Node() throws IOException, ClassNotFoundException {
		id = new Random().nextInt(10 + 1);
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
	public void stabilize(Node node) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				while(true) {
					try {
						System.out.println("Node[" + node.getId() + "] - Ring stabilization routine...");

						Socket client = new Socket("localhost", node.getSucc().getPort());
						ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
						ObjectInputStream in = new ObjectInputStream(client.getInputStream());
						out.writeObject(node.getPort());
						out.writeObject(6);

						Node x = null;
						x = (Node) in.readObject();
						if(x != null)
							if((x.getId() > node.getId() && x.getId() < node.getSucc().getId()) ||
									node.getId() == node.getSucc().getId()) {
								node.setSucc(x);
								System.out.println("Node[" + node.getId() + "] - Successor updated: " + node.getSucc().getId());
							}
						client.close();
						
						node.notifySucc(node.getSucc());

						node.checkPredecessor();

						System.out.println("Node[" + node.getId() + "] - Successor is " + node.getSucc().getId() + 
								", Predecessor is " + (node.getPred() != null ? node.getPred().getId() : null));

						Thread.sleep(1000);
					} catch (InterruptedException | IOException | ClassNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}).start();
	}

	protected void checkPredecessor() {
		try {
			if(pred != null) {
				Socket client = new Socket("localhost", pred.getPort());
				if(!client.isConnected())
					setPred(null);
				client.close();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected void notifySucc(Node node) throws UnknownHostException, IOException {

		Socket client = null;
		ObjectOutputStream out = null;

		client = new Socket("localhost", node.getPort());

		if(client.isConnected()) {
			out = new ObjectOutputStream(client.getOutputStream());
			out.writeObject(port);	//node port
			out.writeObject(5);	//notify
			out.writeObject(this);

		}
		client.close();
	}

	public int getPort() {
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
			if(isa.getPort() != port) {
				client = new Socket("localhost", isa.getPort());
				if(client.isConnected()) {

					out = new ObjectOutputStream(client.getOutputStream());
					in = new ObjectInputStream(client.getInputStream());

					out.writeObject(port);	//node port
					out.writeObject(2);		//request id (join)
					out.writeObject(this);
					pred = null;
					succ = (Node) in.readObject();

					if(succ != null) {
						System.out.println("Node[" + getId() + "] attached to ring." );
						System.out.println("Node[" + getId() + "] - Successor is " + getSucc().getId());
						client.close();
						break;
					}
					else {
						System.err.println(" Node[" + getId() + "] request rejected." );
						client.close();
						break;
					}
				}
			}
		}
		stabilize(this);
	}

	public void addFile() throws IOException {
		File file = new RandomFile().getFile();
		Socket client = new Socket("localhost", this.getPort());
		ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
		ObjectInputStream in = new ObjectInputStream(client.getInputStream());

		out.writeObject(this.getPort());
		out.writeObject(7);
		out.writeObject(this);

		Node k = null;
		try {
			k = (Node) in.readObject();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} finally {
			client.close();
		}

		client = new Socket("localhost", k.getPort());
		out = new ObjectOutputStream(client.getOutputStream());
		out.writeObject(this.getPort());
		out.writeObject(3);
		out.writeObject(file);
		client.close();
	}

	/**
	 * Ring creation
	 *
	 * @throws IOException
	 */
	public void create() throws IOException {
		if(succ == null) {		//if succ == null ring isn't created yet
			pred = null;
			succ = this;

			//finger table initialization
			finger = new Hashtable<>();
			for(int i = 1; i <= p; i++) 
				finger.put(i, this); 

			System.out.println("Node[" + this.getId() + "]: Ring created.");
			System.out.println("Node[" + this.getId() + "]: PredecessorID = " + this.getPred() + ", SuccessorID = " + this.getSucc().getId());
		}
		else
			System.out.println("Ring already created.");
	}

	public Hashtable<Integer, File> getFileList() {
		return fileList;
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@SuppressWarnings("resource")
	@Override
	public void run() {
		try {

			ServerSocket server;
			server = new ServerSocket(port);

			Executor executor = Executors.newFixedThreadPool(100);

			while(true)
				executor.execute(new ServerHandler(server.accept(), this, m));
		} catch (IOException e) {
			System.err.println("Connection error! " + this.getId());
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

	public Hashtable<Integer, Node> getFinger() {
		return finger;
	}


	public void setId(int id) {
		this.id = id;
	}

}
