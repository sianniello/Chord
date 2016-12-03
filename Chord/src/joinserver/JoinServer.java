package joinserver;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import node.Request;

public class JoinServer {

	private ServerSocket server;
	private HashSet<InetSocketAddress> set;

	public JoinServer(int port) throws IOException {
		server = new ServerSocket(port);
		set = new HashSet<>();

		System.out.println("Server listening at: " + port);
	}

	public void execute() throws IOException{

		Socket client =null;
		checkOnline();
		Executor executor = Executors.newFixedThreadPool(1000);

		while(true){
			client = server.accept();
			executor.execute(new ServerHandler(client, set));
		}

	}

	private void checkOnline() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				while(true) {
					synchronized (this) {
						for(InetSocketAddress isa : set)
							try {
								Socket client = new Socket(isa.getHostName(), isa.getPort());
								ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
								out.writeObject(new Request(isa, Request.check_alive));
								client.close();
								Thread.sleep(10000);
								System.out.println(set.toString());
							} catch (IOException | InterruptedException e) {
								set.remove(isa);
								System.err.println(isa + " is offline");
								System.out.println("Network: " + set.toString());
							}
					}
				}
			}
		}).start();
	}

	public static void main(String[] args) throws IOException {

		JoinServer jserver = new JoinServer(1099);

		try {
			jserver.execute();
		} catch (IOException ex) {
			Logger.getLogger(JoinServer.class.getName()).log(Level.SEVERE, null, ex);
		}finally{
			if(! jserver.server.isClosed()) 
				jserver.server.close();
		}
	}


}
