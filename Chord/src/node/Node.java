package node;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.security.PublicKey;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Scanner;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import cryptografy.Cryptography;
import randomFile.RandomFile;

/**
 * This is the main class, its include client role
 *
 * @author Stefano.
 *         Created 23 nov 2016.
 */
public class Node implements Runnable, Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Node succ, succ2, pred;
	private final static int m = 10;		//keys/ID space
	private int id;
	public transient PublicKey tagetPubKey;
	private InetSocketAddress node_address;
	HashFunction hf = Hashing.sha1();
	private ClientHandler ch;
	private HashSet<InetSocketAddress> set;		//Bottomlay network's addresses
	private Hashtable<Integer, File> fileList;
	private Hashtable<Integer, File> replica;
	private File file;
	private boolean online, stab;
	private int k;
	
	/**
	 * only port constructor. it will uses default join server address
	 * @param port
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	@SuppressWarnings({ "javadoc", "unqualified-field-access" })
	public Node(int port) throws IOException, ClassNotFoundException {
		this.node_address = new InetSocketAddress(InetAddress.getLocalHost(), port);
		id = Math.abs(hf.hashString(node_address.toString(), Charset.defaultCharset()).asInt())%m;
		set = new HashSet<>();
		joinServer(null);
		succ = succ2 = null;
		fileList = new Hashtable<>();
		replica = new Hashtable<>();
		online = true;
		stab = false;
		ch = new ClientHandler(this);
		Cryptography.keyGeneration();
	}

	/**
	 * contructor with node's port and join server's address
	 * @param node_port
	 * @param join_server
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	public Node(int node_port, InetSocketAddress join_server) throws ClassNotFoundException, IOException {
		this.node_address = new InetSocketAddress(InetAddress.getLocalHost(), node_port);
		id = Math.abs(hf.hashString(node_address.toString(), Charset.defaultCharset()).asInt())%m;
		set = new HashSet<>();
		joinServer(join_server);
		succ = succ2 = null;
		fileList = new Hashtable<>();
		replica = new Hashtable<>();
		online = true;
		stab = false;
		ch = new ClientHandler(this);
		Cryptography.keyGeneration();
	}

	public synchronized Hashtable<Integer, File> getReplica() {
		return replica;
	}

	public void setReplica(Hashtable<Integer, File> replica) {
		this.replica = replica;
	}

	private void joinServer(InetSocketAddress join_server) throws UnknownHostException, IOException, ClassNotFoundException {
		ch = new ClientHandler(this);
		ch.joinServer(join_server);
	}

	public void setSet(HashSet<InetSocketAddress> set) {
		this.set = set;
	}

	public int getPort() {
		return node_address.getPort();
	}

	/**
	 * Ring creation
	 *
	 * @throws IOException
	 */
	public void create() throws IOException {
		if(succ == null) {		//if succ == null ring isn't created yet
			pred = null;
			succ = succ2 = this;
			Forwarder f = new Forwarder();
			Request req = new Request(node_address, Request.start_stabilize);
			f.send(req);
			System.out.println("Node[" + id + "]: Ring created.");
			System.out.println(this.toString());
		}
		else
			System.out.println("Ring already created.");
	}

	public InetSocketAddress getAddress() {
		return node_address;
	}

	public synchronized Hashtable<Integer, File> getFileList() {
		return fileList;
	}

	/**
	 * Add a runtime-generated random file in 'k-corresponding' node. K is the result
	 * of hashing function of file. 
	 * If k != node's id client's node forwards a request
	 * to its successor.
	 * @throws IOException
	 */
	public synchronized void addFile() throws IOException {
		file = new RandomFile().getFile();
		File encrypted = Cryptography.encrypt(file);
		k = Math.abs(hf.hashBytes(Files.toByteArray(file)).asInt())%m;

		//node is liable of file or node's successor is himself.
		if(k == this.getId() || id == succ.getId()) {

			fileList.put(k, Cryptography.encrypt(file));

			//node send a copy of file to its successor as backup
			if(succ.getId() != this.id)
				new ClientHandler().saveReplica(succ, file, k);
			System.out.println(this.toString() + ": save file " + file.getName() + ", dimension " + file.length() + " bytes, with key " + k);
			System.out.println(this.toString() + ": Filelist " + this.getFileList().toString());
		}
		//node's successor is liable of file. 
		else if(k == succ.getId() || successor(succ.getId(), id, k))
			new ClientHandler().addFile(succ, Cryptography.encrypt(file), k);
		else 
			new ClientHandler().addFileReq(succ, k, this);
	}

	/**
	 * once target node is found it can be saved in its list
	 * @param node = target node for saving file
	 */
	public void saveFile(Node node) {
		ch = new ClientHandler();
		ch.addFile(node, Cryptography.encrypt(file), k);
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		try {
			ServerSocket server = new ServerSocket(node_address.getPort());

			Executor executor = Executors.newFixedThreadPool(1000);
			while(online) {
				Socket client = server.accept();
				executor.execute(new ServerHandler(client, this, m));
			}
			server.close();
		} catch (IOException e) {
			System.err.println("Connection with sever lost! " + this.toString() + " " + e.getClass().toString());
		}
	}

	public synchronized HashSet<InetSocketAddress> getSet() {
		return set;
	}

	public synchronized int getId() {
		return id;
	}

	public synchronized Node getSucc() {
		return succ;
	}

	public synchronized void setSucc(Node succ) {
		this.succ = succ;
	}

	public synchronized Node getPred() {
		return pred;
	}

	public synchronized void setPred(Node pred) {
		this.pred = pred;
	}

	public File getFile() {
		return file;
	}

	public static void main(String[] args) throws IOException {
		Node n = null;
		int choice = 0;
		Scanner scanner = new Scanner(System.in);

		System.out.println("Enter node port (range 10000-10100): ");
		int node_port = scanner.nextInt();
		System.out.println("Enter JoinServer port:");
		int js_port = scanner.nextInt();

		try {
			n = new Node(node_port, InetSocketAddress.createUnresolved("localhost", js_port));
		} catch (NumberFormatException | ClassNotFoundException e) {
			System.err.println("Invalid address format");
			scanner.close();
			return ;
		}

		new Thread(n, "Node[" + n.getId() + "]").start();
		while(choice != 7) {
			System.out.println("Choose operation");
			System.out.println("-------------------------\n");
			System.out.println("1 - Create ring");
			System.out.println("2 - Join Ring");
			System.out.println("3 - Add a file");
			System.out.println("4 - File list");
			System.out.println("5 - Replica list");
			System.out.println("6 - Change port");
			System.out.println("7 - Go offline");

			choice = scanner.nextInt();
			System.out.println("\nYour ID: " + n.getId() + "\n");
			switch (choice) {
			case 1:
				n.create();
				break;
			case 2:
				int i = 1;
				System.out.println("\n***Online nodes***");
				for(InetSocketAddress isa : n.getSet()) {
					if(isa.getPort() != n.getPort())
						System.out.println(i + ". " + isa.toString());
					i++;
				}
				System.out.println("Enter node index");
				int node = scanner.nextInt();
				n.joinRing(node);
				break;
			case 3:
				n.addFile();
				break;
			case 4:
				System.out.println(n.toString() + " Filelist:" + n.getFileList());
				scanner.hasNext();
				break;
			case 5:
				System.out.println(n.toString() + " Replica:" + n.getReplica());
				scanner.hasNext();
				break;
			case 6:
				System.out.println("Enter node port (range 10000-10100): ");
				n.setPort(scanner.nextInt());
				break;
			case 7:
				n.setOffline();
				break;
			default:
				// The user input an unexpected choice.
				break;
			}
		}
		scanner.close();
	}

	private void setPort(int nextInt) {
		try {
			node_address = new InetSocketAddress(InetAddress.getLocalHost(), nextInt);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Node wants to enter the ring through	target node
	 * @param node = traget node
	 */
	public void joinRing(int node) {
		ch = new ClientHandler(this);
		ch.joinRequest(node);
	}

	public String toString() {
		if(pred == null && succ != null)
			return "Node[addr="+ node_address + ", ID=" + id + ", SuccID=" + succ.getId() + ", PredID=null]";
		else if(pred == null && succ == null)
			return "Node[addr="+ node_address + ", ID=" + id + ", SuccID=null , PredID=null]"; 
		else if(getPred() != null)
			return "Node[addr="+ node_address + ", ID=" + id + ", SuccID=" + succ.getId() + ", PredID=" + pred.getId() + "]"; 
		else return "Node[addr="+ node_address + ", ID="+ id + ", SuccID=" + succ.getId() + ", PredID=null";
	}

	public synchronized boolean isOnline() {
		return online;
	}

	public void setOffline() {
		System.out.println(this.toString() + " goes offline...");
		online = false;
		stab = false;
	}

	public void setStabilization(boolean stab) {
		this.stab = stab;
	}

	public boolean getStabilization() {
		return stab;
	}

	public synchronized void saveReplicaFile(int k2, File file2) {
		replica.put(k2, file2);
	}

	/**
	 * After a join new node receive its boot list from its successor
	 * @param fileList2
	 */
	public synchronized void reassignment(Hashtable<Integer, File> fileList2) {
		fileList.putAll(fileList2);
		System.out.println(fileList);
	}

	public synchronized Node getSucc2() {
		return succ2;
	}

	public synchronized void setSucc2(Node succ2) {
		this.succ2 = succ2;
	}

	public synchronized void saveReplicaList(Hashtable<Integer, File> replicaList) {
		replica.putAll(replicaList);
	}

	public PublicKey getPubKey() {
		return Cryptography.getPubKey();
	}

	public void setPubKeyTarget(PublicKey k) {
		new Cryptography().setPubKeyTarget(k);
	}
	
	/**
	 * This function verify if x in (n, s)
	 */
	boolean successor(int s, int n, int x) {
		if(x == n || x == s) return false;
		if((s - n + m)%m > (x - n + m)%m || n == s)
			return true;
		else return false;
	}

}