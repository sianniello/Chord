package node;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.Hashtable;
import java.util.Random;

class ServerHandler implements Runnable {

	private ObjectInputStream in = null;
	private static Node n;
	public int m;
	private Request req;

	public ServerHandler(Socket client, Node n, int m) throws IOException {
		in = new ObjectInputStream(client.getInputStream());
		ServerHandler.n = n;
		this.m = m;
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void run() {
		try {
			Request request = (Request) in.readObject();

			switch(request.getRequest()) {

			//node receives an add file request. It can accept or forwards request on ring
			case Request.addFile_REQ:
				int k = request.getK();

				//file is under responsability of this node. So it sends back its identity to sender node
				if(k == n.getId())	
					new Forwarder().send(new Request(request.getNode().getAddress(), Request.addFile_RES, n));

				//file is under responsability of current node's successor
				else if(k == n.getSucc().getId() || successor(n.getSucc().getId(), n.getId(), k))	
					new Forwarder().send(new Request(request.getNode().getAddress(), Request.addFile_RES, n.getSucc()));

				//request is forwording
				else	
					new Forwarder().send(new Request(n.getSucc().getAddress(), Request.addFile_REQ, k, request.getNode()));
				break;

				//node receives liable node of file saving
			case Request.addFile_RES:
				n.saveFile(request.getNode());
				break;

				//node receives file to save in its list also sends a replica of file to its successor
			case Request.addFile:
				int key = request.getK();
				n.getFileList().put(key, request.getFile());
				if(n.getSucc() != null && n.getId() != n.getSucc().getId())
					new Forwarder().send(new Request(n.getSucc().getAddress(), Request.replicaFile, key, request.getFile()));
				System.out.println(n.toString() + ": save file " + request.getFile().getName() + ", dimension " + 
						request.getFile().length() + " bytes with key " + key);
				System.out.println(n.toString() + ": Filelist " + n.getFileList().toString());
				break;

				//a new node requests join to a ring's node. It sends back successor of new node
			case Request.join_REQ:
				//node receives a join request but it has not joined any ring yet
				if(n.getSucc() == null) {
					System.err.println("No ring running");
					break;
				}

				System.out.println(n.toString() + ": received a join request from Node[" + request.getNode().getAddress() + "]");
				if(request.getNode().getId() == n.getSucc().getId() || successor(n.getSucc().getId(), n.getId(), request.getNode().getId()))
					new Forwarder().send(new Request(request.getNode().getAddress(), Request.join_RES, n.getSucc()));
				else
					new Forwarder().send(new Request(n.getSucc().getAddress(), Request.join_REQ, request.getNode()));
				break;

				//joined node update its succ and start stabilization routine
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

				//start stabilization signal received
			case Request.start_stabilize:
				stabilize(n);
				break;

				//node sends stabilization request to its successor and it reply with its predecessor
			case Request.stabilize_REQ:
				Forwarder f = new Forwarder();
				req = new Request(request.getNode().getAddress(), Request.stabilize, n.getPred());
				f.send(req);
				break;

				//node receives its successor's predecessor called 'x', check it and notifyies its successor
			case Request.stabilize:
				Node x = request.getNode();
				if(x != null && (successor(n.getSucc().getId(), n.getId(), x.getId()) || n.getSucc().getId() == n.getId())) {
					n.setSucc(x);
					System.out.println(n.toString() + ": Successor updated, now it's " + n.getSucc().getId());
					new Forwarder().send(new Request(n.getSucc().getAddress(), Request.replicaList, n.getFileList()));
					if(n.getPred() != null)
						new Forwarder().send(new Request(n.getPred().getAddress(), Request.succ2Update, n.getSucc()));
				}
				notifySuccessor();
				break;

				//node receives a notify message from x. It become aware that a new node just entered right behind him
			case Request.notify:
				x = request.getNode();
				if(n.getPred() == null || (n.getPred().getId() == n.getId() || predecessor(n.getPred().getId(), x.getId(), n.getId()))) {

					if(!n.getFileList().isEmpty()){
						Hashtable<Integer, File> copy = (Hashtable<Integer, File>) n.getFileList().clone();
						Hashtable<Integer, File> reassign = new Hashtable<>();
						for(int key2 : copy.keySet()) {
							if(x.getPred() == null && (successor(x.getId(), n.getId(), key2) || key2 == x.getId())) {
								reassign.put(key2, copy.get(key2));
								n.getFileList().remove(key2);
							}
							else if(n.getPred() != null && (key2 == x.getId()  || predecessor(n.getPred().getId(), key2, x.getId()))) {
								reassign.put(key2, copy.get(key2));
								n.getFileList().remove(key2);
							}
						}
						//node sends to joined node a list of files with k-corresponding to joined node's id
						new Forwarder().send(new Request(x.getAddress(), Request.reassign_file, reassign));
						System.out.println(n.toString() + ": Filelist " + n.getFileList());
					}
					n.setPred(x);
					n.getReplica().clear();
					System.out.println(n.toString() + ": Predecessor updated, now it's " + n.getPred().getId());
				}
				break;

				//node receives file from its predecessor then save it to its replica list
			case Request.replicaFile:
				n.saveReplicaFile(request.getK(), request.getFile());
				break;

				//node receives file LIST from its predecessor then save it to its replica list
			case Request.replicaList:
				n.saveReplicaList(request.getFileList());
				break;

				//node after done a join receives file LIST from its successor then saves it to its list
			case Request.reassign_file:
				n.reassignment(request.getFileList());
				break;

			case Request.check_alive:
				//dummy request
				break;

			case Request.succ2Update:
				n.setSucc2(request.getNode());
				break;
				
			case Request.pubKey_REQ:
				new Forwarder().send(new Request(request.getNode().getAddress(), Request.pubKey_RES, n.getPubKey()));
				break;
			case Request.pubKey_RES:
				n.setPubKeyTarget(request.getPubKey());
				break;
				
			default:
				break;
			}
		} catch (IOException | ClassNotFoundException e) {
			System.err.println(n.toString() + ": Connection err!");
			e.printStackTrace();
		} 
	}


	/**
	 * node notify its successor that it's right behind it
	 */
	private void notifySuccessor() {
		if(n.getSucc() != null && n.isOnline() && n.getId() != n.getSucc().getId()) {
			Forwarder f = new Forwarder();
			Request req = new Request(n.getSucc().getAddress(), Request.notify, n);
			try {
				f.send(req);
			} catch (IOException e) {
				//successor failed
			}
		}
	}

	/**
	 * Called periodically. n asks the successor
	 * about its predecessor, verifies if n's immediate
	 * successor is consistent, and tells the successor about n
	 * when in while loop function dedicates thread to stabilize routine
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
					if(n.getSucc2() != null) {
						System.err.println(n.toString() + ": Successor has FAILED.");
						n.setSucc(n.getSucc2());
						System.out.println(n.toString() + ": new successor is " + n.getSucc().getId());
					}
				}

				if(node.getPred() != null)
					check_predecessor();

				//System.out.println(node.toString() + ": stabilization routine...");
				try {
					Thread.sleep(new Random().nextInt(5000) + 2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * called periodically. checks whether predecessor has failed.
	 * @param node
	 */
	public void check_predecessor() {
		if(n.isOnline() && n.getPred() != null) 
			try {
				new Forwarder().sendCheck(new Request(n.getPred().getAddress(), Request.check_alive, n));
			} catch (IOException e) {
				//predecessor has failed!
				System.err.println(n.getPred().toString() + " HAS FAILED.");
				n.setPred(null);
				n.getFileList().putAll(n.getReplica());
				System.out.println(n.toString() + " File list recovered: " + n.getFileList());
				n.getReplica().clear();
			}
	}


	/**
	 * This function verifies if x in (n, s)
	 */
	boolean successor(int s, int n, int x) {
		if(x == n || x == s) return false;
		if((s - n + m)%m > (x - n + m)%m || n == s)
			return true;
		else return false;
	}

	/**
	 * This function verifies if x in (p, n)
	 */
	boolean predecessor(int p, int x, int n) {
		if(x == n || x == p) return false;
		if((n - p + m)%m > (n - x + m)%m)
			return true;
		else return false;
	}

}