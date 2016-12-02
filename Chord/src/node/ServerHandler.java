package node;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;
import java.util.TreeMap;

class ServerHandler implements Runnable {

	private ObjectOutputStream out = null;
	private ObjectInputStream in = null;
	private static Node n;
	public int m;
	private TreeMap<Integer, Node> ring;
	private Request req;

	public ServerHandler(Socket client, Node n, int m, TreeMap<Integer, Node> ring) throws IOException {
		in = new ObjectInputStream(client.getInputStream());
		this.n = n;
		this.m = m;
		this.ring = ring;
	}

	public ServerHandler() {

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
				System.out.println(n.toString() + ": save file " + request.getFile().getName());
				break;

				//a new node request join to a ring's node. It send back successor of new node
			case Request.join_request:
				System.out.println(n.toString() + ": received a join request from Node[" + request.getNode().getId() + "]");
				Node succ = findSuccessor(request.getNode().getId());
				if(!n.getRing().containsKey(request.getNode().getId()))
					synchronized (this) {
						n.getRing().put(request.getNode().getId(), request.getNode());
					}
				new Forwarder().send(new Request(request.getNode().getPort(), Request.join, succ));
				break;

				//joined node update his succ and start stabilization routine
			case Request.join:
				if(request.getNode() == null)
					System.err.println("Join Failed!");
				if(request.getNode().getId() == n.getId())
					System.err.println("Join Failed! ID already running on ring.");
				else {
					n.setSucc(request.getNode());
					n.setPred(null);
					System.out.println(n.toString() + ": join ring");
					stabilize(n);
				}
				break;

			case Request.start_stabilize:
				stabilize(n);
				break;

				//node send stabilization request to his successor and it reply with his predecessor
			case Request.stabilize_request:
				Forwarder f = new Forwarder();
				req = new Request(request.getNode().getPort(), Request.stabilize, n.getPred());
				f.send(req);
				break;

				//node receives his successor's predecessor called 'x', check it and notifyies his successor
			case Request.stabilize:
				Node x = request.getNode();
				if(x != null && n.getSucc().getId() == n.getId())
					n.setSucc(x);
				if(x != null && successor(n.getSucc().getId(), n.getId(), x.getId())) {
					n.setSucc(x);
					System.out.println(n.toString() + ": Successor updated, now it's " + n.getSucc().getId());
				}
				notifySuccessor();
				break;

				//node receive a notify message
			case Request.notify:
				x = request.getNode();
				if(n.getPred() != null && n.getPred().getId() == n.getId())
					n.setPred(x);
				if(n.getPred() == null || predecessor(n.getPred().getId(), x.getId(), n.getId())) {
					n.setPred(x);
					System.out.println(n.toString() + ": Predecessor updated, now it's " + n.getPred().getId());
				}
				break;

			case Request.check_alive:
				break;

			default:
				break;
			}
		} catch (IOException | ClassNotFoundException e) {
			System.err.println("Connection lost!" + n.toString());
			e.printStackTrace();
		} 
	}

	private void notifySuccessor() {
		if(n.isOnline()) {
			Forwarder f = new Forwarder();
			Request req = new Request(n.getSucc().getPort(), Request.notify, n);
			try {
				f.send(req);
			} catch (IOException e) {
				System.err.println(n.getSucc().toString() + " HAS FAILED.");
				if(n.getRing().containsKey(n.getSucc().getId()))
					synchronized (this) {
						n.getRing().remove(n.getSucc().getId());
					}
				n.setSucc(n.findSuccessor(n.getId()));
				System.out.println(n.toString() + ": new successor is " + n.getSucc().toString());
			}
		}
	}

	Node findSuccessor(int k) {
		TreeMap<Integer, Node> aux = new TreeMap<>();
		for(int id : ring.keySet())
			aux.put((id - k + m)%m, ring.get(id));
		return aux.get(aux.firstKey());
	}

	/**
	 * called periodically. n asks the successor
	 * about its predecessor, verifies if n's immediate
	 * successor is consistent, and tells the successor about n
	 * 
	 * @param node
	 */
	public void stabilize(Node node) {
		if(!n.getStabilization() && n.isOnline())
			new Thread(new Runnable() {
				public void run() {
					node.setStabilization(true);
					while(node.getStabilization() && node.isOnline()) {
						Forwarder f = new Forwarder();
						req = new Request(node.getSucc().getPort(), Request.stabilize_request, node);
						try {
							f.send(req);
						} catch (IOException e1) {
							System.err.println(node.getSucc().toString() + " HAS FAILED.");
							if(n.getRing().containsKey(n.getSucc().getId()))
								synchronized (this) {
									n.getRing().remove(n.getSucc().getId());
								}
							n.setSucc(n.findSuccessor(n.getId()));
							System.out.println(node.toString() + ": new successor is " + node.getSucc().toString());
						}

						if(node.getPred() != null)
							check_predecessor(node);

						System.out.println(node.toString() + ": stabilization routine...");
						try {
							Thread.sleep(new Random().nextInt(5000) + 2000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}, node.toString() + ": stabilizator").start();
	}

	public void check_predecessor(Node node) {
		if(n.isOnline())
			(new Runnable() {
				@Override
				public void run() {
					if(n.getPred() != null) {
						Request req = new Request(n.getPred().getPort(), Request.check_alive, n);
						Forwarder f = new Forwarder();
						if(!f.sendCheck(req)) {
							System.err.println(n.getPred().toString() + " HAS FAILED.");
							if(n.getRing().containsKey(n.getPred().getId()))
								synchronized (this) {
									n.getRing().remove(n.getPred().getId());
								}
							n.getFileList().putAll(n.getPred().getFileList());
							System.err.println(n.toString() + " file list recovered.");
							n.setPred(null);
						}
					}
				}
			}).run();
	}

	boolean successor(int s, int n, int x) {
		if(x == n || x == s) return false;
		if((s - n + m)%m > (x - n + m)%m)
			return true;
		else return false;
	}

	boolean predecessor(int p, int x, int n) {
		if(x == n || x == p) return false;
		if((n - p + m)%m > (n - x + m)%m)
			return true;
		else return false;
	}

}
