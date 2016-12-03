package node;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Hashtable;
import java.util.Random;

class ServerHandler implements Runnable {

	private ObjectOutputStream out = null;
	private ObjectInputStream in = null;
	private static Node n;
	public int m;
	private Request req;

	public ServerHandler(Socket client, Node n, int m) throws IOException {
		in = new ObjectInputStream(client.getInputStream());
		this.n = n;
		this.m = m;
	}

	public ServerHandler() {

	}

	public ServerHandler(Node n) throws IOException {
		this.n = n;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void run() {
		try {
			Request request = (Request) in.readObject();

			switch(request.getRequest()) {

			case Request.addFile_REQ:
				int k = request.getK();
				if(k == n.getId())
					new Forwarder().send(new Request(request.getNode().getAddress(), Request.addFile_RES, n));
				else if(k == n.getSucc().getId() || successor(n.getSucc().getId(), n.getId(), k))
					new Forwarder().send(new Request(request.getNode().getAddress(), Request.addFile_RES, n.getSucc()));
				else
					new Forwarder().send(new Request(n.getSucc().getAddress(), Request.addFile_REQ, k, request.getNode()));
				break;

			case Request.addFile_RES:
				n.saveFile(request.getNode());
				break;

			case Request.addFile:
				int key = request.getK();
				n.getFileList().put(key, request.getFile());
				System.out.println(n.toString() + ": save file " + request.getFile().getName() + " with key " + key);
				System.out.println(n.toString() + ": Filelist " + n.getFileList().toString());
				break;

				//a new node request join to a ring's node. It send back successor of new node
			case Request.join_REQ:
				System.out.println(n.toString() + ": received a join request from Node[" + request.getNode().getAddress() + "]");
				if(request.getNode().getId() == n.getSucc().getId() || successor(n.getSucc().getId(), n.getId(), request.getNode().getId()))
					new Forwarder().send(new Request(request.getNode().getAddress(), Request.join_RES, n.getSucc()));
				else
					new Forwarder().send(new Request(n.getSucc().getAddress(), Request.join_REQ, request.getNode()));
				break;

				//joined node update his succ and start stabilization routine
			case Request.join_RES:
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
			case Request.stabilize_REQ:
				Forwarder f = new Forwarder();
				req = new Request(request.getNode().getAddress(), Request.stabilize, n.getPred());
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
				if(n.getPred() == null)
					n.setPred(n);
				if(n.getId() != x.getId() && (n.getPred().getId() == n.getId() || predecessor(n.getPred().getId(), x.getId(), n.getId()))) {
					if(!n.getFileList().isEmpty()){
						Hashtable<Integer, File> copy = (Hashtable<Integer, File>) n.getFileList().clone();
						Hashtable<Integer, File> reassign = new Hashtable<>();
						for(int key2 : copy.keySet()) 
							if(predecessor(n.getPred().getId(), key2, x.getId()))
								reassign.put(key2, copy.get(key2));
						new Forwarder().send(new Request(x.getAddress(), Request.reassign, reassign));
					}
					n.setPred(x);

					System.out.println(n.toString() + ": Predecessor updated, now it's " + n.getPred().getId());
				}
				break;

			case Request.replica:
				n.saveReplica(request.getK(), request.getFile());
				break;

			case Request.reassign:
				n.Reassignment(request.getFileList());
				break;

			case Request.check_alive:
				//dummy request
				break;

			default:
				break;
			}
		} catch (IOException | ClassNotFoundException e) {
			System.err.println(n.toString() + ": Connection err!");
			e.printStackTrace();
		} 
	}

	private void notifySuccessor() {
		if(n.isOnline()) {
			Forwarder f = new Forwarder();
			Request req = new Request(n.getSucc().getAddress(), Request.notify, n);
			try {
				f.send(req);
			} catch (IOException e) {
				//TODO successor failed
			}
		}
	}

	/**
	 * Called periodically. n asks the successor
	 * about its predecessor, verifies if n's immediate
	 * successor is consistent, and tells the successor about n
	 * 
	 * @param node
	 */
	public void stabilize(Node node) {
		if(!n.getStabilization() && n.isOnline()) {
			node.setStabilization(true);
			while(node.getStabilization() && node.isOnline()) {
				Forwarder f = new Forwarder();
				req = new Request(node.getSucc().getAddress(), Request.stabilize_REQ, node);
				try {
					f.send(req);
				} catch (IOException e1) {
					//TODO successor failed
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
	}

	/**
	 * This function verify in node is online. Predecessor node in particular
	 * @param node
	 */
	public void check_predecessor(Node node) {
		Request req = null;
		if(n.isOnline() && n.getPred() != null)
			req = new Request(n.getPred().getAddress(), Request.check_alive, n);
		Forwarder f = new Forwarder();
		if(!f.sendCheck(req)) {
			System.err.println(n.getPred().toString() + " HAS FAILED.");
			//TODO recover file procedure
			n.setPred(null);
		}
	}


	/**
	 * This function verify if x in (n, s)
	 */
	boolean successor(int s, int n, int x) {
		if(x == n || x == s) return false;
		if((s - n + m)%m > (x - n + m)%m || n == s)
			return true;
		else return false;
	}

	/**
	 * This function verify if x in (p, n)
	 */
	boolean predecessor(int p, int x, int n) {
		if(x == n || x == p) return false;
		if((n - p + m)%m > (n - x + m)%m)
			return true;
		else return false;
	}

}