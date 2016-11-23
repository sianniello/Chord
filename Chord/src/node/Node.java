package node;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

import jdk.internal.dynalink.beans.StaticClass;

/**
 * TODO Put here a description of what this class does.
 *
 * @author Stefano.
 *         Created 23 nov 2016.
 */
public class Node implements Runnable{

	private static final int max = 11000, min = 10000;
	private int succ, pred;
	private int id;
	private int port;

	/**
	 * @param 
	 */
	@SuppressWarnings({ "javadoc", "unqualified-field-access" })
	public Node(int port) throws IOException, ClassNotFoundException {
		id = Integer.hashCode(port);
		this.port = port;
	}

	protected void stabilize() {
		new Runnable() {
			
			@Override
			public void run() {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
		};
		
	}

	@SuppressWarnings({ "javadoc", "resource" })
	public void join() throws IOException, ClassNotFoundException{
		Socket client = null;
		ObjectOutputStream out = null;
		ObjectInputStream in = null;

		int t = min;

		do {
			client = new Socket("localhost", t);
			t++;
		} while(!client.isConnected() || t > max);

			if(t == max) System.out.println("No ring found");

			else {
				System.out.println("Connection estabilished with node " + client.getPort());
				
				out = new ObjectOutputStream(client.getOutputStream());
				in = new ObjectInputStream(client.getInputStream());

				out.writeObject(port);
				out.writeObject(2);
				succ = (Integer) in.readObject();
			}
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
			case 4:	//TODO addFile(); break;
			default: System.err.println("Wrong input");

			s.close();
			}
		}
	}

	@SuppressWarnings("resource")
	private void create() throws IOException {
		if(succ == 0) {
			pred = 0;
			succ = this.getId();
			System.out.println("Ring created. Successor ID = " + succ);
			stabilize();
			ServerSocket server = new ServerSocket(port);
			System.out.println("ServerHandler is listenig at port " + port);

			Executor executor = Executors.newFixedThreadPool(1500);

			while(true){
				executor.execute(new ServerHandler(server.accept()));
			}
		}
		else
			System.out.println("Ring already created.");
	}

	@SuppressWarnings("resource")
	@Override
	public void run() {
		try {
			join();

			ServerSocket server;
			server = new ServerSocket(port);

			//(new Thread(new ClientHandler(port))).start();

			Executor executor = Executors.newFixedThreadPool(10);

			while(true)
				executor.execute(new ServerHandler(server.accept()));
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	
	public int getId() {
		return id;
	}

}
