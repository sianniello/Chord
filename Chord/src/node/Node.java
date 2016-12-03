package node;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Random;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;

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
	private final static int m = 10;		//keys/ID space
	private int id;
	private InetSocketAddress node_address;
	HashFunction hf = Hashing.sha1();
	private ClientHandler ch;
	private HashSet<InetSocketAddress> set;		//Bottomlay network's addresses
	private Hashtable<Integer, File> fileList;
	private File file;
	private boolean online, stab, recovery;
	private int k;
	private String server;
	private int server_port;

	@SuppressWarnings({ "javadoc", "unqualified-field-access" })
	public Node(int port) throws IOException, ClassNotFoundException {
		this.node_address = new InetSocketAddress(InetAddress.getLocalHost(), port);
		id = Math.abs(hf.hashString(node_address.toString(), Charset.defaultCharset()).asInt())%m;
		set = new HashSet<>();
		joinServer(null);
		succ = null;
		fileList = new Hashtable<>();
		online = true;
		stab = false;
		recovery = false;
		ch = new ClientHandler(this);
	}

	public Node(int node_port, InetSocketAddress join_server) throws ClassNotFoundException, IOException {
		this.node_address = new InetSocketAddress(InetAddress.getLocalHost(), node_port);
		id = Math.abs(hf.hashString(node_address.toString(), Charset.defaultCharset()).asInt())%m;
		set = new HashSet<>();
		joinServer(join_server);
		succ = null;
		fileList = new Hashtable<>();
		online = true;
		stab = false;
		recovery = false;
		ch = new ClientHandler(this);
	}

	public boolean isRecovery() {
		return recovery;
	}

	public void setRecovery(boolean recovery) {
		this.recovery = recovery;
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
			succ = this;
			Forwarder f = new Forwarder();
			Request req = new Request(node_address, Request.start_stabilize);
			f.send(req);
			System.out.println("Node[" + this.getId() + "]: Ring created.");
			System.out.println(this.toString());
		}
		else
			System.out.println("Ring already created.");
	}

	public InetSocketAddress getAddress() {
		return node_address;
	}

	public Hashtable<Integer, File> getFileList() {
		return fileList;
	}

	public void addFile() throws IOException {
		file = new RandomFile().getFile();
		k = Math.abs(hf.hashBytes(Files.toByteArray(file)).asInt())%m;

		if(k == this.getId()) {
			fileList.put(k, file);
			System.out.println(this.toString() + ": save file " + file.getName() + " with key " + k);
			System.out.println(this.toString() + ": Filelist " + this.getFileList().toString());
		}
		else 
			new ClientHandler(this).addFileReq(k);
	}

	public void saveFile(Node node) {
		ch = new ClientHandler(this);
		ch.addFile(node, file, k);
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

	public static void main(String[] args) throws IOException {

		int choice = 0;
		Scanner scanner = new Scanner(System.in);

		System.out.println("Enter node port (range 10000-10100): ");
		int node_port = scanner.nextInt();
		System.out.println("Enter node port JoinServer address (address:p) or leave blank to default");
		String input = scanner.next();
		scanner.close();
		String addr[] = input.split(":");
		
		Node n;
		try {
			n = new Node(node_port, InetSocketAddress.createUnresolved(addr[0], Integer.parseInt(addr[1])));
		} catch (NumberFormatException | ClassNotFoundException e) {
			System.err.println("Invalid address format");
			return ;
		}
		
		new Thread(n, "Node[" + n.getId() + "]").start();
		while(choice != 4) {
			System.out.println("Choose operation");
			System.out.println("-------------------------\n");
			System.out.println("1 - Create ring");
			System.out.println("2 - Join Ring");
			System.out.println("3 - Add a file");
			System.out.println("4 - Go offline");

			choice = scanner.nextInt();
			System.out.println("Your ID: " + n.getId() + "\n");
			switch (choice) {
			case 1:
				n.create();
				break;
			case 2:
				System.out.println("Enter node");
				int node = scanner.nextInt();
				n.joinRing(node);
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

	private void setJoinServer(String ip, int port) {
		
	}

	public void joinRing(int node) {
		ch = new ClientHandler(this);
		ch.joinRequest(node);
	}

	public void setId(int id) {
		this.id = id;
	}

	public String toString() {
		if(pred == null && succ != null)
			return "Node[addr="+ node_address + ", ID=" + id + ", SuccID=" + succ.getId() + ", PredID=null]";
		else if(pred == null && succ == null)
			return "Node[addr="+ node_address + ", ID=" + id + ", SuccID=null , PredID=null]"; 
		else
			return "Node[addr="+ node_address + ", ID=" + id + ", SuccID=" + succ.getId() + ", PredID=" + pred.getId() + "]"; 
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

	public boolean getRecovery() {
		return recovery;
	}

}
