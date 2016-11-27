package node;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import com.google.common.hash.Hashing;

class ServerHandler implements Runnable {

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
			System.out.println("Node[" + request.getClient_port() + "]: contacts ServerSide of Node[" + n.getPort() + "] for " + request.getRequest());

			switch(request.getRequest()) {
			case 2: //join
				int client_id = Hashing.consistentHash(request.getClient_port(), m);
				System.out.println("Node[" + n.getId() + "]: Node[" + client_id + "] requests join.");

				if(n.getSucc() != null) {	//if ring exist
					Node node = request.getNode();
					out.writeObject(findSuccessor(node));	//send new node its successor
					if(n.getSucc().getId() == n.getId()) {
						n.setSucc(node);
						System.out.println("Node[" + n.getId() + "] - Successor updated: " + n.getSucc().getId());
					}
				}
				else
					out.writeObject(null);
				out.flush();
				break;
			case 3: //add file
				File file = request.getFile();
				addFile(file);
				out.flush();
				break;
			case 5:	//notify
				succNotify(request.getNode());
				break;
			case 6:	//stabilize
				out.writeObject(n.getPred());
				out.flush();
				break;
			case 7:	//find_successor
				Node id = request.getNode();
				out.writeObject(findSuccessor(id));
				out.flush();
				break;
			}
		} catch (IOException | ClassNotFoundException e) {
			System.err.println("Connection lost!");
			e.printStackTrace();
		}
	}

	private void addFile(File file) throws ClassNotFoundException, IOException {
		n.getFileList().put(Hashing.consistentHash(file.hashCode(), m), file);
		System.out.println("Node[" + n.getId() + "]: adds file '" + file.getName() + "'. Size = " + file.length() + " bytes");
	}

	private void succNotify(Node n1) {
		if((n.getPred() == null || n.getPred().getId() == n.getId()) || (n1.getId() > n.getPred().getId() && n1.getId() < n.getId())) {
			n.setPred(n1);
			System.out.println("Node[" + n.getId() + "] - Successor is " + n.getSucc().getId() + ", Predecessor is " + n.getPred().getId());
		}
	}

	public Node findSuccessor(Node node) {
		if(n.getId() == n.getSucc().getId() || node.getId() == n.getId()) 
			return n;
		else if (n.getId() > n.getSucc().getId() && n.getId() < node.getId())
			return n.getSucc();
		else return findSuccessor(n.getSucc());
	}
}
