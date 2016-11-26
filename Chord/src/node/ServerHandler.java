package node;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import com.google.common.hash.Hashing;

class ServerHandler implements Runnable {

	@SuppressWarnings("unused")
	private Socket client;
	private ObjectOutputStream out;
	private ObjectInputStream in;
	private Node n;
	private int m;

	public ServerHandler(Socket client, Node n, int m) throws IOException {
		this.client = client;
		out = new ObjectOutputStream(client.getOutputStream());
		in = new ObjectInputStream(client.getInputStream());
		this.n = n;
		this.m = m;
	}

	@Override
	public void run() {
		int client_port = 0;
		try {
			client_port = (Integer) in.readObject();
			System.out.println("Node[" + client_port + "]: contacts ServerSide of Node[" + n.getPort() + "]");

			switch((Integer) in.readObject()) {
			case 2: //join
				int client_id = Hashing.consistentHash(client_port, m);
				System.out.println("Node[" + n.getId() + "]: Node[" + client_id + "] requests join.");
				if(n.getSucc() != null) {	//if ring exist
					Node node = (Node) in.readObject();
					out.writeObject(findSuccessor(node));	//send new node its successor
					n.stabilize(n);
					if(node.getId() > n.getSucc().getId())
						n.setSucc(node);
				}
				else
					out.writeObject(null);
				break;
			case 3: //add file
				addFile();
				break;
			case 5:	//notify
				succNotify((Node) in.readObject());
				break;
			case 6:	//stabilize
				out.writeObject(n.getPred());
				break;
			case 7:	//find_successor
				Node id = (Node) in.readObject();
				out.writeObject(findSuccessor(id));
				break;
			}
		} catch (IOException | ClassNotFoundException e) {
			System.err.println("Connection lost!");
			e.printStackTrace();
		}
	}

	private void addFile() throws ClassNotFoundException, IOException {
		File file = (File) in.readObject();
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
		if(n.getId() == n.getSucc().getId() || node.getId() <= n.getId()) 
			return n;
		else return findSuccessor(n.getSucc());
	}
}
