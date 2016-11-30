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
	 * Simple request delivery via port
	 * in stabilization routine if successor is offline the request is forwarded to successor of successor
	 *
	 * @param message
	 * @param port
	 * @throws IOException
	 */
	@SuppressWarnings({ "unqualified-field-access" })
	public void send(Request request) throws IOException {

		//Fail management in stabilize routine
		if (request.getRequest() == Request.stabilize) {
			Socket client = new Socket("localhost", request.getPort());
			if(!client.isConnected()) {
				client.close();
				client = new Socket("localhost", request.getNode().getSucc().getPort());
			}
			out = new ObjectOutputStream(client.getOutputStream());
			out.writeObject(request);
			client.close();
		}
		else {
			Socket client = new Socket("localhost", request.getPort());
			out = new ObjectOutputStream(client.getOutputStream());
			out.writeObject(request);
			client.close();
		}
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

