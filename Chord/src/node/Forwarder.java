package node;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * @author Stefano.
 */
public class Forwarder {

	private ObjectOutputStream out;
	private Socket client;

	public Forwarder() {
	}

	/**
	 * Simple request delivery via port
	 * in stabilization routine if successor is offline the request is forwarded to successor of successor
	 *
	 * @param request
	 * @throws UnknownHostException 
	 * @throws IOException
	 */
	@SuppressWarnings({ "unqualified-field-access" })
	public synchronized void send(Request request) throws UnknownHostException, IOException {
		client = new Socket(request.getAddress().getHostName(), request.getAddress().getPort());
		client.setSoTimeout(1000);
		out = new ObjectOutputStream(client.getOutputStream());
		out.writeObject(request);
		client.close();
	}

	public synchronized void send(Request request, Node node) throws UnknownHostException, IOException {
		client = new Socket(request.getAddress().getHostName(), request.getAddress().getPort());
		out = new ObjectOutputStream(client.getOutputStream());
		out.writeObject(request);
		client.close();
	}

	public synchronized void sendCheck(Request request) throws UnknownHostException, IOException {
		client = new Socket(request.getAddress().getHostName(), request.getAddress().getPort());
		client.setSoTimeout(1000);
		out = new ObjectOutputStream(client.getOutputStream());
		out.writeObject(request);
		client.close();
	}

}

