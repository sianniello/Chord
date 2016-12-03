package node;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashSet;

/**
 * @author Stefano.
 */
public class Forwarder {

	private ObjectOutputStream out;
	private Socket client;
	private InetSocketAddress address;

	@SuppressWarnings("javadoc")
	public Forwarder() {
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
		client = new Socket(request.getAddress().getHostName(), request.getAddress().getPort());
		client.setSoTimeout(1000);
		out = new ObjectOutputStream(client.getOutputStream());
		out.writeObject(request);
		client.close();
	}

	public void send(Request request, Node node) throws UnknownHostException, IOException {
		client = new Socket(request.getAddress().getHostName(), request.getAddress().getPort());
		out = new ObjectOutputStream(client.getOutputStream());
		out.writeObject(request);
		client.close();
	}

	public boolean sendCheck(Request request) {
		try {
			client = new Socket(request.getAddress().getHostName(), request.getAddress().getPort());
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

