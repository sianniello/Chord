package node;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashSet;

/**
 * @author Stefano.
 */
public class Forwarder {

	private HashSet<Node> ring;
	private ObjectOutputStream out;

	@SuppressWarnings("javadoc")
	public Forwarder() {
	}

	@SuppressWarnings({ "javadoc" })
	public Forwarder(HashSet<Node> ring) {
		this.ring = ring;
	}

	/**
	 * Spedizione di un messaggio semplice attraverso una data porta.
	 *
	 * @param message
	 * @param port
	 * @throws IOException
	 */
	@SuppressWarnings({ "unqualified-field-access" })
	public void send(Request request) throws IOException {
		Socket client = new Socket("localhost", request.getPort());
		out = new ObjectOutputStream(client.getOutputStream());
		out.writeObject(request);
		client.close();
	}

	/**
	 * Broadcast su tutto il ring.
	 *
	 * @param message
	 * @throws IOException
	 */
	@SuppressWarnings("unqualified-field-access")
	public void sendAll(Request request) throws IOException {
		for(Node i : ring) {
			Socket client = new Socket("localhost", i.getId());
			out = new ObjectOutputStream(client.getOutputStream());
			out.writeObject(request);
			client.close();
		}
	}

	public void send(Request request, Node node) throws UnknownHostException, IOException {
		Socket client = new Socket("localhost", node.getPort());
		out = new ObjectOutputStream(client.getOutputStream());
		out.writeObject(request);
		client.close();
	}

}

