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
import java.util.TreeMap;
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

	protected static final int notify = 0;
	public static final int add_file = 3;
	public static final int join = 2;
	public static final int stabilize = 6;
	public static final int find_successor = 7;

	private Node succ, pred;
	private final static int m = 8;		//keys/ID space
	private int id;
	private int port;
	private HashSet<InetSocketAddress> set;		//Bottomlay network's addresses
	private Hashtable<Integer, File> fileList;
	private  static TreeMap<Integer, Node> ring = new TreeMap<>();
	private File file;

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

	/**
	 * called periodically. n asks the successor
	 * about its predecessor, verifies if n's immediate
	 * successor is consistent, and tells the successor about n
	 * 
	 * @param node
	 */
	public void stabilize(Node node) {
		new Thread(new Runnable() {
			public void run() {
				while(true) {
					try {
						Forwarder f = new Forwarder();
						Request req = new Request(node.getSucc().getPort(), Request.stabilize, node);
						f.send(req);
						Thread.sleep(new Random().nextInt(5000) + 2000);
					} catch (InterruptedException | IOException e) {
						e.printStackTrace();
					}
				}
			}
		}).start();
	}

	protected void neighborUpdate(Node down) {
		if(succ.getId() == down.getId())
			succ = down.getSucc();
		if(pred.getId() == down.getId())
			pred = down.getPred();
	}

	public int getPort() {
		return port;
	}

	/**
	 * @param node 
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	@SuppressWarnings({ "javadoc" })
	public void join() throws IOException {
		Forwarder f = new Forwarder();
		Request request = new Request(ring.get(ring.firstKey()).getPort(), Request.join_request, this);
		f.send(request);
	}

	public TreeMap<Integer, Node> getRing() {
		return ring;
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
			if(!ring.containsValue(this)) {
				synchronized (this) {
					ring.put(this.getId(), this);
				}
			}
			System.out.println("Node[" + this.getId() + "]: Ring created.");
			System.out.println(this.toString());
			//stabilize(this);
		}
		else
			System.out.println("Ring already created.");
	}

	public Hashtable<Integer, File> getFileList() {
		return fileList;
	}

	public void saveFile() throws IOException {
		file = new RandomFile().getFile();
		int k = Hashing.consistentHash(file.hashCode(), m);
		Node n = findSuccessor(k);
		Forwarder f = new Forwarder();
		Request request = new Request(n.getPort(), Request.save_file, file);
		try {
			f.send(request, this);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	Node findSuccessor(int k) {
		TreeMap<Integer, Node> aux = new TreeMap<>();
		for(int id : ring.keySet())
			aux.put((id - k + m)%m, ring.get(id));
		return aux.get(aux.firstKey());
	}

	public void addToRing(Node n) {
		ring.put(n.getId(), n);
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
				executor.execute(new ServerHandler(client, this, m, ring));
			}
		} catch (IOException e) {
			System.err.println("Connection lost! " + this.toString());
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

	public File getFile() {
		return file;
	}

	public void setFile(File f) {
		this.file = f;
	}

	public static void main(String[] args) throws ClassNotFoundException, IOException {

	}

	public void setId(int id) {
		this.id = id;
	}

	public String toString() {
		if(pred == null && succ != null)
			return "Node[port="+ port + ", ID=" + id + ", SuccID=" + succ.getId() + ", PredID=null]";
		else if(pred == null && succ == null)
			return "Node[port="+ port + ", ID=" + id + ", SuccID=null , PredID=null]"; 
		else
			return "Node[port="+ port + ", ID=" + id + ", SuccID=" + succ.getId() + ", PredID=" + pred.getId() + "]"; 
	}

}
