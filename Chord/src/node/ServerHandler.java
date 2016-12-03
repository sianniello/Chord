package node;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;
import java.util.TreeMap;

import com.google.common.hash.Hashing;

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

	@Override
	public void run() {
		try {
			Request request = (Request) in.readObject();

			switch(request.getRequest()) {

			case Request.addFile_REQ:
				int k = request.getK();
				if(k == n.getId())
					new Forwarder().send(new Request(request.getNode().getPort(), Request.addFile_RES, n));
				else if(k == n.getSucc().getId() || successor(n.getSucc().getId(), n.getId(), k))
					new Forwarder().send(new Request(request.getNode().getPort(), Request.addFile_RES, n.getSucc()));
				else
					new Forwarder().send(new Request(n.getSucc().getPort(), Request.addFile_REQ, k, request.getNode()));
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
				System.out.println(n.toString() + ": received a join request from Node[" + request.getNode().getPort() + "]");
				if(request.getNode().getId() == n.getSucc().getId() || successor(n.getSucc().getId(), n.getId(), request.getNode().getId()))
					new Forwarder().send(new Request(request.getNode().getPort(), Request.join_RES, n.getSucc()));
				else
					new Forwarder().send(new Request(n.getSucc().getPort(), Request.join_REQ, request.getNode()));
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

			case Request.recovery_REQ:
				if(n.getId() == request.getNode().getId())
					new Forwarder().send(new Request(request.getNode().getPort(), Request.recovery_RES, n));
				else if(successor(n.getSucc().getId(), n.getId(), request.getNode().getId()))
					new Forwarder().send(new Request(request.getNode().getPort(), Request.recovery_RES, n.getSucc()));
				else
					new Forwarder().send(new Request(n.getSucc().getPort(), Request.recovery_REQ, request.getNode()));
				break;

			case Request.recovery_RES:
				n.setRecovery(false);
				n.setSucc(request.getNode());
				System.out.println(n.toString() + ": Find new successor, now it's " + n.getSucc().getId());
				break;

			case Request.check_alive:
				//dummy request
				break;

			default:
				break;
			}
		} catch (IOException | ClassNotFoundException e) {
			System.err.println(n.toString() + ": Connection err!");
			//e.printStackTrace();
		} 
	}

	private void notifySuccessor() {
		if(n.isOnline()) {
			Forwarder f = new Forwarder();
			Request req = new Request(n.getSucc().getPort(), Request.notify, n);
			try {
				f.send(req);
			} catch (IOException e) {
				if(!n.getRecovery()) {
					n.setRecovery(true);
					for(InetSocketAddress isa : n.getSet())
						try {
							new Forwarder().send(new Request(isa.getPort(), Request.recovery_REQ, n));
						} catch (IOException e1) {
						}
				}
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
		if(!n.getStabilization() && n.isOnline())
			new Thread(new Runnable() {
				public void run() {
					node.setStabilization(true);
					while(node.getStabilization() && node.isOnline()) {
						Forwarder f = new Forwarder();
						req = new Request(node.getSucc().getPort(), Request.stabilize_REQ, node);
						try {
							f.send(req);
						} catch (IOException e1) {
							if(!n.getRecovery()) {
								n.setRecovery(true);
								for(InetSocketAddress isa : n.getSet())
									try {
										new Forwarder().send(new Request(isa.getPort(), Request.recovery_REQ, n));
									} catch (IOException e2) {
									}
							}
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
		if(n.isOnline() && n.getPred() != null)
			(new Runnable() {
				@Override
				public void run() {
					Request req = new Request(n.getPred().getPort(), Request.check_alive, n);
					Forwarder f = new Forwarder();
					if(!f.sendCheck(req)) {
						System.err.println(n.getPred().toString() + " HAS FAILED.");
						n.getFileList().putAll(n.getPred().getFileList());
						System.err.println(n.toString() + ": file list recovered.");
						System.err.println(n.getFileList().toString());
						n.setPred(null);
					}
				}
			}).run();
	}


	/**
	 * This function verify if x appartiene (n, s)
	 */
	boolean successor(int s, int n, int x) {
		if(x == n || x == s) return false;
		if((s - n + m)%m > (x - n + m)%m || n == s)
			return true;
		else return false;
	}

	/**
	 * This function verify if x appartiene (p, n)
	 */
	boolean predecessor(int p, int x, int n) {
		if(x == n || x == p) return false;
		if((n - p + m)%m > (n - x + m)%m)
			return true;
		else return false;
	}

}
