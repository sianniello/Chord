package node;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashSet;

/**
 * @author Stefano.
 */
public class Forwarder {

	private HashSet<Node> ring;
	private ObjectOutputStream out;
	private Socket client;

	@SuppressWarnings("javadoc")
	public Forwarder() {
	}

	@SuppressWarnings({ "javadoc" })
	public Forwarder(HashSet<Node> ring) {
		this.ring = ring;
	}

	/**
	 * Simple request delivery via port
	 * in stabilization routine if successor is offline the request is forwarded to successor of successor
	 *
	 * @param message
	 * @param port
	 * @throws UnknownHostException 
	 * @throws IOException
	 */
	@SuppressWarnings({ "unqualified-field-access" })
	public void send(Request request) throws UnknownHostException, IOException {
		client = new Socket("localhost", request.getPort());
		client.setSoTimeout(1000);
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
	public void sendAll(Request request) {
		for(Node i : ring) {
			try {
				client = new Socket("localhost", i.getId());
				out = new ObjectOutputStream(client.getOutputStream());
				out.writeObject(request);
				client.close();
			} catch (IOException e) {
				System.err.println(request.getNode().toString());
				e.printStackTrace();
			}
		}
	}

	public void send(Request request, Node node) throws UnknownHostException, IOException {
		client = new Socket("localhost", node.getPort());
		out = new ObjectOutputStream(client.getOutputStream());
		out.writeObject(request);
		client.close();
	}

	public boolean sendCheck(Request request) {
		try {
			client = new Socket("localhost", request.getPort());
			client.setSoTimeout(1000);
			out = new ObjectOutputStream(client.getOutputStream());
			out.writeObject(request);
			client.close();
		} catch (IOException e) {
			return false;
		}
		return true;
	}

}

