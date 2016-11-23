package peer;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TODO Put here a description of what this class does.
 *
 * @author Stefano.
 *         Created 05 nov 2016.
 */
public class Peer implements Runnable{

	private int port;

	/**
	 * @param port porta del peer
	 */
	@SuppressWarnings({ "javadoc", "unqualified-field-access" })
	public Peer(int port) throws IOException, ClassNotFoundException {
		this.port = port;
	}

	/**
	 * il peer contatta il joinserver per farsi inviare la lista dei suoi vicini
	 */
	@SuppressWarnings({ "javadoc", "resource" })
	public void join() throws IOException, ClassNotFoundException{
		Socket client = null;
		ObjectOutputStream out = null;
		ObjectInputStream in = null;

		client = new Socket("localhost", 1099);	//il peer contatta il joinserver
		out = new ObjectOutputStream(client.getOutputStream());
		in = new ObjectInputStream(client.getInputStream());

		out.writeObject(new InetSocketAddress(port));	//il client invia al joinserver il proprio indirizzo
	}

	public static void main(String[] args) {

		if(args.length<1){
			System.err.println("BAD USAGE: inserisci porta");
			System.exit(0);
		}

		try {
			Peer peer = new Peer(Integer.parseInt(args[0]));
		} catch (IOException | ClassNotFoundException ex) {
			Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
		}

	}

	@SuppressWarnings("resource")
	@Override
	public void run() {
		try {
			join();

			ServerSocket server;
			server = new ServerSocket(port);

			(new Thread(new ClientHandler(port))).start();

			Executor executor = Executors.newFixedThreadPool(10);

			while(true)
				executor.execute(new ServerHandler(server.accept(), port));
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

}
