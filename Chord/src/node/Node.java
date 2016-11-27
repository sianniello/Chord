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
import com.sun.security.ntlm.Client;

import randomFile.RandomFile;

/**
 * TODO Put here a description of what this class does.
 *
 * @author Stefano.
 *         Created 23 nov 2016.
 */
@SuppressWarnings("serial")
public class Node implements Runnable, Serializable{

	public static final int add_file = 3;
	public static final int join = 2;
	public static final int notify = 5;
	public static final int stabilize = 6;
	public static final int find_successor = 7;

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

				Socket client = null;
				ObjectOutputStream out = null;
				ObjectInputStream in = null;

				while(true) {
					if(node.getId() != node.getSucc().getId()) {
						System.out.println("Node[" + node.getId() + "] - Ring stabilization routine...");
						try {
							client = new Socket("localhost", node.getSucc().getPort());
							System.out.println(client.toString());
							out = new ObjectOutputStream(client.getOutputStream());
							in = new ObjectInputStream(client.getInputStream());

							Request req = new Request(node.getPort(), stabilize, node);

							out.writeObject(req);
							out.flush();
							Node x = (Node) in.readObject();

							if(x != null)
								if((x.getId() > node.getId() && x.getId() < node.getSucc().getId()) ||
										node.getId() == node.getSucc().getId()) {
									node.setSucc(x);
									System.out.println("Node[" + node.getId() + "] - Successor updated: " + node.getSucc().getId());
								}

							node.notifySucc(node.getSucc());

							node.checkPredecessor();

							System.out.println("Node[" + node.getId() + "] - Successor is " + node.getSucc().getId() + 
									", Predecessor is " + (node.getPred() != null ? node.getPred().getId() : null));

						} catch (IOException | ClassNotFoundException e) {
							e.printStackTrace();
						}finally {
							try {
								client.close();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
					try {
						Thread.sleep(new Random().nextInt(3000) + 1000);
					} catch (InterruptedException e) {
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
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected void notifySucc(Node node) throws IOException {

		Socket client = null;
		ObjectOutputStream out = null;

		try {
			client = new Socket("localhost", node.getPort());

			if(client.isConnected()) {
				out = new ObjectOutputStream(client.getOutputStream());

				Request req = new Request(port, notify, this);
				out.writeObject(req);	//notify
				out.flush();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public int getPort() {
		return port;
	}

	/**
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	@SuppressWarnings({ "javadoc" })
	public void join() throws IOException {
		Socket client = null;
		ObjectOutputStream out = null;
		ObjectInputStream in = null;

		//node tries to connect to ring
		for (InetSocketAddress isa : set) {
			if(isa.getPort() != port) {
				try {
					client = new Socket("localhost", isa.getPort());

					out = new ObjectOutputStream(client.getOutputStream());
					in = new ObjectInputStream(client.getInputStream());

					Request req = new Request(port, join, this);
					out.writeObject(req);
					out.flush();
					succ = (Node) in.readObject();
					pred = null;

				}catch (IOException | ClassNotFoundException e) {
					e.printStackTrace();
				}finally {
					in.close();
					out.close();
				}

				if(succ != null) {
					System.out.println("Node[" + getId() + "] attached to ring." );
					System.out.println("Node[" + getId() + "] - Successor is " + getSucc().getId());
					stabilize(this);
					break;
				}
				else {
					System.err.println("Node[" + getId() + "] request rejected." );
				}
			}
		}
	}

	public void addFile() throws IOException {
		File file = new RandomFile().getFile();
		Socket client = new Socket("localhost", this.getPort());
		ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
		ObjectInputStream in = new ObjectInputStream(client.getInputStream());

		Request req = new Request(this.getPort(), find_successor, this);

		out.writeObject(req);

		Node k = null;
		try {
			k = (Node) in.readObject();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		client = new Socket("localhost", k.getPort());
		out = new ObjectOutputStream(client.getOutputStream());

		req = new Request(port, add_file, file);
		out.writeObject(req);

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
		stabilize(this);
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

			while(true) {
				Socket client = server.accept();
				executor.execute(new ServerHandler(client, this, m));
			}
		} catch (IOException e) {
			System.err.println("Connection error! " + this.toString());
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

	public String toString() {
		return "Node[port="+ port + ", id=" + id + "]"; 
	}

}
