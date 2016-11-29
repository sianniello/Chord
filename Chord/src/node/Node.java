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
import java.util.HashSet;
import java.util.Hashtable;
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

	public static final int add_file = 3;
	public static final int join = 2;
	public static final int stabilize = 6;
	public static final int find_successor = 7;

	private Node succ, pred;
	private final static int m = 8;		//keys/ID space
	private static final int p = 3;		//finger table's entries
	protected static final int notify = 0;
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
		client.close();
		System.out.println("Node[" + port + "] - Network: " + set.toString());
	}

	public Node findSuccessor(int id) {
		if(this.getId() == this.getSucc().getId())
			return this;
		else if((this.getSucc().getId() + m - this.getId())%m >= (id + m - this.getId())%m)
			return this.getSucc();
		else return this.getSucc().findSuccessor(id);
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

			Socket client = null;
			ObjectOutputStream out = null;
			ObjectInputStream in = null;

			@Override
			public void run() {
				try {
					client = new Socket("localhost", node.getSucc().getPort());
					if(!client.isConnected())
						client = new Socket("localhost", node.getSucc().getSucc().getPort());

					out = new ObjectOutputStream(client.getOutputStream());
					in = new ObjectInputStream(client.getInputStream());

					while(true) {
						System.out.println("Node[" + node.getId() + "]: Ring stabilization routine...");
						if(node.getId() != node.getSucc().getId() || node.getPred() != null) {
							//receive predecessor of successor
							out.writeObject(new Request(stabilize, node));
							Node x = (Node) in.readObject();
							if(x != null)
								if((node.getSucc().getId() + m - node.getId())%m > x.getId() + m - node.getId() || node.getId() == node.getSucc().getId()) {
									node.setSucc(x);
									in.close();
									out.close();
									client.close();
									client = new Socket("localhost", node.getSucc().getPort());
									out = new ObjectOutputStream(client.getOutputStream());
									in = new ObjectInputStream(client.getInputStream());
									out.writeObject(new Request(stabilize, node));
									x = (Node) in.readObject();
									System.out.println("Node[" + node.getId() + "]: Successor updated: " + node.getSucc().getId());
								}

							System.out.println("Node[" + node.getId() + "]: Successor is " + node.getSucc().getId() + 
									", Predecessor is " + (node.getPred() != null ? node.getPred().getId() : null));
						}
						out.writeObject(new Request(0));
						checkPredecessor(node);
						Thread.sleep(new Random().nextInt(5000) + 2000);
					}
				} catch (IOException | ClassNotFoundException | InterruptedException e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	protected void checkPredecessor(Node node) {
		(new Runnable() {

			@Override
			public void run() {
				ObjectOutputStream out;
				if(pred != null)
					try {
						Socket client_pred = new Socket("localhost", node.getPred().getPort());
						if(client_pred.isConnected()) {
							out = new ObjectOutputStream(client_pred.getOutputStream());
							out.writeObject(new Request(0));
						}
						else
							setPred(null);
						client_pred.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
			}
		}).run();
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

					out.writeObject(new Request(join, this));
					succ = (Node) in.readObject();
					pred = null;
					client.close();

				}catch (IOException | ClassNotFoundException e) {
					e.printStackTrace();
				}

				if(succ != null) {
					System.out.println("Node[" + getId() + "]: Attached to ring." );
					System.out.println("Node[" + getId() + "]: Successor is " + getSucc().getId());
					stabilize(this);
					break;
				}
				else {
					System.err.println("Node[" + getId() + "]: Request rejected." );
				}
			}
		}
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
			out.flush();
			out.close();
			client.close();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
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
			stabilize(this);
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

			Executor executor = Executors.newFixedThreadPool(1000);
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
