package peer;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

class ServerHandler implements Runnable {

	private Socket client;
	private ObjectOutputStream out;
	private ObjectInputStream in;
	public ServerHandler(Socket client, int port) throws IOException {
		this.client = client;
		out = new ObjectOutputStream(client.getOutputStream());
		in = new ObjectInputStream(client.getInputStream());

	}

	@Override
	public void run() {

		try {
			String message = (String) in.readObject();
			System.out.println("Lato Server Peer " +  ": ho ricevuto" + message);
			out.writeObject(message);

		} catch (IOException | ClassNotFoundException ex) {
			Logger.getLogger(ServerHandler.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

}
