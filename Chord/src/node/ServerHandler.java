package node;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import com.google.common.hash.Hashing;

class ServerHandler implements Runnable {

	public static final int add_file = 3;
	public static final int join = 2;
	public static final int stabilize = 6;
	public static final int find_successor = 7;
	private static final int notify = 0;

	private ObjectOutputStream out = null;
	private ObjectInputStream in = null;
	private Node n;
	private int m;

	public ServerHandler(Socket client, Node n, int m) throws IOException {
		out = new ObjectOutputStream(client.getOutputStream());
		in = new ObjectInputStream(client.getInputStream());
		this.n = n;
		this.m = m;
	}

	public ServerHandler(Node n) throws IOException {
		this.n = n;
	}

	@Override
	public void run() {
		try {
			Request request = (Request) in.readObject();
			System.out.println("Node[" + n.getId() + "]: " + request.toString());
			switch(request.getRequest()) {
			case join: //join
				int client_id = Hashing.consistentHash(request.getNode().getPort(), m);
				System.out.println("Node[" + n.getId() + "]: Node[" + client_id + "]: Requests join.");

				if(n.getSucc() != null) {	//if ring exist
					Node node = request.getNode();
					out.writeObject(n.findSuccessor(node.getId()));	//send new node its successor
				}
				else
					out.writeObject(null);
				break;
			case add_file:
				System.out.println("Node[" + n.getId() + "]: Add file.");
				File file = request.getFile();
				addFile(file);
				break;
			case stabilize:
				Node n1 = null;
				do {
					out.writeObject(n.getPred());
					n1 = (Node) in.readObject();
					succNotify(n1);
				} while(n1 != null);
			case find_successor:
				Node node = request.getNode();
				out.writeObject(n.findSuccessor(node.getId()));
				break;
			default:
				break;
			}
		} catch (IOException | ClassNotFoundException e) {
			//System.err.println("Connection lost!" + n.toString());
			//e.printStackTrace();
		} 
	}

	private void addFile(File file) throws ClassNotFoundException, IOException {
		n.getFileList().put(Hashing.consistentHash(file.hashCode(), m), file);
		System.out.println("Node[" + n.getId() + "]: adds file '" + file.getName() + "'. Size = " + file.length() + " bytes");
	}

	private void succNotify(Node n1) {
		if(n1 != null)
			if(n1.getId() != n.getId())
				if(n.getPred() == null || (n.getPred().getId() + m - n.getId())%m < (n1.getId() + m - n.getId())%m) {
					n.setPred(n1);
					System.out.println("Node[" + n.getId() + "]: Predecessor changed now its " + n.getPred().getId());
				}
	}

}
