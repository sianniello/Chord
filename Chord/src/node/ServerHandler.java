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
		while(true) {
			int client_port = 0;
			try {
				client_port = (Integer) in.readObject();
				System.out.println("Node[" + client_port + "]connected.");

				switch((Integer) in.readObject()) {
				case 2: //join
					System.out.println("Node[" + client_port + "] requests join.");
					if(n.getSucc() != null) {	//if ring exist
						Node node = (Node) in.readObject();
						int id = Hashing.consistentHash(client_port, m);
						out.writeObject(findSuccessor(node));	//send new node its successor
					}
					else
						out.writeObject(null);
					client.close();
					break;
				case 3: //add file
					addFile();
				case 5:	//notify
					succNotify((Node) in.readObject());
					break;
				}
			} catch (IOException | ClassNotFoundException e) {
				System.err.println("Connection lost!");
				e.printStackTrace();
			}
		}
	}

	private void addFile() throws ClassNotFoundException, IOException {
		File file = (File) in.readObject();
		n.getFileList().add(file);
	}

	private void succNotify(Node n1) {
		if(n.getPred() == null || (n1.getId() > n.getPred().getId() && n1.getId() < n.getId()))
			n.setPred(n1);
	}

	public Node findSuccessor(Node node) {
		if(n.getId() == n.getSucc().getId()) 
			return node;

		Node n0;
		if (node.getId() > n.getId() && node.getId() <= n.getSucc().getId()) 
			return n.getSucc();
		else
			n0 = closestPrecedingNode(node.getId());
		return findSuccessor(n0);
	}

	private Node closestPrecedingNode(int id) {
		for(int i = m; i == 1; i--)
			if(n.getFinger().get(i).getId() > n.getId() && n.getFinger().get(i).getId() < id)
				return n.getFinger().get(i);
		return n;
	}

}
