package node;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.HandshakeCompletedEvent;

import jdk.internal.dynalink.beans.StaticClass;
import randomFile.RandomFile;

/**
 * TODO Put here a description of what this class does.
 *
 * @author Stefano.
 *         Created 23 nov 2016.
 */
public class Node implements Runnable{

	private Node succ, pred;
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


	private int id;
	private int port;
	private HashSet<InetSocketAddress> set;

	@SuppressWarnings({ "javadoc", "unqualified-field-access" })
	public Node(int port) throws IOException, ClassNotFoundException {
		id = Integer.hashCode(port);
		this.port = port;
		new ClientHandler(port, set).run();
		succ = null;
	}

	protected void stabilize() {
		new Runnable() {

			@Override
			public void run() {
				try {
					System.out.println("Ring stabilization routine...");
					Thread.sleep(10000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		};

	}

	/**
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	@SuppressWarnings({ "javadoc", "resource" })
	public void join() throws IOException, ClassNotFoundException{
		Socket client = null;
		ObjectOutputStream out = null;
		ObjectInputStream in = null;

		for (InetSocketAddress isa : set) {
			client = new Socket("localhost", isa.getPort());
			if(client.isConnected()) break;
		}

		System.out.println("Connection estabilished with node " + client.getPort());

		out = new ObjectOutputStream(client.getOutputStream());
		in = new ObjectInputStream(client.getInputStream());

		out.writeObject(port);	//node port
		out.writeObject(2);		//request id (join)
		succ = (Node) in.readObject();
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

	private void addFile() throws IOException {
		File f = new RandomFile().getFile();

	}

	public void create() throws IOException {
		if(succ == null) {
			pred = null;
			succ = this;
			System.out.println("Ring created. Successor ID = " + succ.getId());
			stabilize();

		}
		else
			System.out.println("Ring already created.");
	}

	@SuppressWarnings("resource")
	@Override
	public void run() {
		try {

			ServerSocket server;
			server = new ServerSocket(port);

			Executor executor = Executors.newFixedThreadPool(10);

			while(true)
				executor.execute(new ServerHandler(server.accept(), this));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	public int getId() {
		return id;
	}

	public Node findSucc(int id) {
		if (id == this.id) 
			return this;
		else
			return succ.findSucc(id);
	}

}
