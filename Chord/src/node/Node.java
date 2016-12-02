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
import java.util.TreeMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.google.common.hash.Hashing;
import randomFile.RandomFile;
import sun.font.CreatedFontTracker;

/**
 * This is the main class, its include client role
 *
 * @author Stefano.
 *         Created 23 nov 2016.
 */
@SuppressWarnings("serial")
public class Node implements Runnable, Serializable{

	private Node succ, pred;
	private final static int m = 8;		//keys/ID space
	private int id;
	private int port;
	private ClientHandler ch;
	private HashSet<InetSocketAddress> set;		//Bottomlay network's addresses
	private Hashtable<Integer, File> fileList;
	private  static TreeMap<Integer, Node> ring = new TreeMap<>();
	private File file;
	private boolean online, stab;

	@SuppressWarnings({ "javadoc", "unqualified-field-access" })
	public Node(int port) throws IOException, ClassNotFoundException {
		id = Hashing.consistentHash(port, m);
		set = new HashSet<>();
		this.port = port;
		joinServer();
		succ = null;
		fileList = new Hashtable<>();
		online = true;
		stab = false;
		ch = new ClientHandler(this);
	}

	public Node() throws IOException, ClassNotFoundException {
		id = new Random().nextInt(10 + 1);
		succ = null;
		online = true;
	}


	@SuppressWarnings("unchecked")
	private void joinServer() throws UnknownHostException, IOException, ClassNotFoundException {
		ch = new ClientHandler(this);
		ch.joinServer();
	}

	public void setSet(HashSet<InetSocketAddress> set) {
		this.set = set;
	}

	public int getPort() {
		return port;
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
			Forwarder f = new Forwarder();
			Request req = new Request(this.getPort(), Request.start_stabilize);
			f.send(req);
			if(!ring.containsValue(this)) {
				synchronized (this) {
					ring.put(this.getId(), this);
				}
			}
			System.out.println("Node[" + this.getId() + "]: Ring created.");
			System.out.println(this.toString());
		}
		else
			System.out.println("Ring already created.");
	}

	public Hashtable<Integer, File> getFileList() {
		return fileList;
	}

	public void addFile() throws IOException {
		file = new RandomFile().getFile();
		int k = Hashing.consistentHash(file.hashCode(), m);

		if(k == this.getId())
			fileList.put(k, file);
		else {
			ch = new ClientHandler(this);
			ch.addFileReq(k);
		}
	}

	public void saveFile(Node node) {
		ch = new ClientHandler(this);
		ch.addFile(node, file);
	}

	public void addToRing(Node n) {
		ring.put(n.getId(), n);
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		try {
			ServerSocket server = new ServerSocket(port);

			Executor executor = Executors.newFixedThreadPool(1000);
			while(online) {
				Socket client = server.accept();
				executor.execute(new ServerHandler(client, this, m, ring));
			}
			server.close();
		} catch (IOException e) {
			System.err.println("Connection lost! " + this.toString() + " " + e.getClass().toString());
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

		int choice = 0;
		Scanner scanner = new Scanner(System.in);

		System.out.println("Enter node port (range 10000-10100): ");
		Node n = new Node(scanner.nextInt());
		new Thread(n, "Node[" + n.getId() + "]").start();
		while(choice != 4) {
			System.out.println("Choose operation");
			System.out.println("-------------------------\n");
			System.out.println("1 - Create ring");
			System.out.println("2 - Join Ring");
			System.out.println("3 - Add a file");
			System.out.println("4 - Go offline");

			choice = scanner.nextInt();

			switch (choice) {
			case 1:
				n.create();
				break;
			case 2:
				System.out.println("Enter join Node port: ");
				int port = scanner.nextInt();
				n.joinRing(port);
				break;
			case 3:
				n.addFile();
				break;
			case 4:
				n.setOffline();
				break;
			default:
				// The user input an unexpected choice.
			}
		}
		scanner.close();
	}

	private void joinRing(int port) {
		if(port > 10100 && port < 10000)
			port = -1;
		ch = new ClientHandler(this);
		ch.joinRequest(port);
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

	public boolean isOnline() {
		return online;
	}

	public void setOffline() {
		System.out.println(this.toString() + " goes offline...");
		this.online = false;
		this.stab = false;
	}

	public void setStabilization(boolean stab) {
		this.stab = stab;
	}

	public boolean getStabilization() {
		return stab;
	}

}
