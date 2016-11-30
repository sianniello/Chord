package node;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Hashtable;
import java.util.TreeMap;

class ServerHandler implements Runnable {

	private ObjectOutputStream out = null;
	private ObjectInputStream in = null;
	private Node n;
	private int m;
	private TreeMap<Integer, Node> ring;

	public ServerHandler(Socket client, Node n, int m, TreeMap<Integer, Node> ring) throws IOException {
		out = new ObjectOutputStream(client.getOutputStream());
		in = new ObjectInputStream(client.getInputStream());
		this.n = n;
		this.m = m;
		this.ring = ring;
	}

	public ServerHandler(Node n) throws IOException {
		this.n = n;
	}

	@Override
	public void run() {
		try {
			Request request = (Request) in.readObject();

			switch(request.getRequest()) {
			case Request.save_file:
				n.getFileList().put(n.getId(), request.getFile());
				System.out.println("Node[" + n.getId() + "]: save file " + request.getFile().getName());
				break;
			case Request.join_request:
				System.out.println("Node[" + n.getId() + "]: received a join request");
				Node succ = findSuccessor(request.getNode().getId());
				if(!n.getRing().containsKey(request.getNode().getId()))
					synchronized (this) {
						n.getRing().put(request.getNode().getId(), request.getNode());
					}
				new Forwarder().send(new Request(request.getNode().getPort(), Request.join, succ));
				n.stabilize(n);
				break;
				
			case Request.join:
				System.out.println(n.toString() + ": join ring");
				n.setSucc(request.getNode());
				n.setPred(null);
				n.stabilize(n);
				break;
				
			case Request.stabilize_request:
				Forwarder f = new Forwarder();
				Request req = new Request(request.getNode().getPort(), Request.stabilize, n.getPred());
				f.send(req);
				break;
				
			case Request.stabilize:
				Node x = request.getNode();
				if((n.getSucc().getId() - n.getId() + m)%m > (x.getId() - n.getId() + m)%m) {
					n.setSucc(x);
					synchronized (this) {
						n.getRing().replace(n.getId(), n);
					}
					System.out.println("Node[" + n.getId() + "]: Successor updated, now it's " + n.getSucc().getId());
				}
				notifySuccessor();
				break;
				
			case Request.notify:
				x = request.getNode();
				if(n.getPred() == null || ((n.getPred().getId() - n.getId() + m)%m < (x.getId() - n.getId() + m)%m)) {
					n.setPred(x);
					synchronized (this) {
						n.getRing().replace(n.getId(), n);
					}
					System.out.println("Node[" + n.getId() + "]: Predecessor updated, now it's " + n.getPred().getId());
				}
				break;
				
			default:
				break;
			}
		} catch (IOException | ClassNotFoundException e) {
			System.err.println("Connection lost!" + n.toString());
			e.printStackTrace();
		} 
	}

	private void notifySuccessor() throws IOException {
		Forwarder f = new Forwarder();
		Request req = new Request(n.getSucc().getPort(), Request.notify, n);
		f.send(req);
	}

	Node findSuccessor(int k) {
		TreeMap<Integer, Node> aux = new TreeMap<>();
		for(int id : ring.keySet())
			aux.put((id - k + m)%m, ring.get(id));
		return aux.get(aux.firstKey());
	}

}
