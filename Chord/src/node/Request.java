package node;

import java.io.File;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.Hashtable;

@SuppressWarnings("serial")
public class Request implements Serializable{

	public static final int addFile_REQ = 1;
	public static final int addFile_RES = 2;
	public static final int addFile = 3;
	public static final int join_REQ = 4;
	public static final int join_RES = 5;
	public static final int start_stabilize = 6;
	public static final int stabilize_REQ = 7;
	public static final int stabilize = 8;
	public static final int notify = 9;
	public static final int check_alive = 0;
	public static final int replicaFile = 10;
	public static final int reassign = 11;
	public static final int replica_REQ = 12;
	public static final int replicaList = 13;
	public static final int pubKey_REQ = 14;
	public static final int pubKey_RES = 15;


	private InetSocketAddress address;	//destination address
	private int request;	//request type
	private Node node;	//source node
	private File file;
	private int k;	//key of file
	private Hashtable<Integer, File> fileList;

	public Request(InetSocketAddress address) {
		this.address = address;
	}

	public Request(InetSocketAddress address, int request) {
		this.address = address;
		this.request = request;
	}

	public Request(InetSocketAddress address, int request, File file) {
		this(address, request);
		this.file = file;
	}

	public Request(InetSocketAddress address, int request, Node node) {
		this(address, request);
		this.node = node;
	}

	public Request(InetSocketAddress address, int request, int k, Node node) {
		this(address, request, node);
		this.k = k;
	}

	public Request(InetSocketAddress address, int request, int k, File file) {
		this(address, request, file);
		this.k = k;
	}
	public Request(InetSocketAddress address, int request, Hashtable<Integer, File> fileList) {
		this(address, request);
		this.fileList = fileList;
	}

	//change key request
	public Request(InetSocketAddress address2, int pubkeyRes, String pubKey) {
		// TODO Auto-generated constructor stub
	}

	public InetSocketAddress getAddress() {
		return address;
	}
	public void setAddress(InetSocketAddress address) {
		this.address = address;
	}
	public int getRequest() {
		return request;
	}
	public void setRequest(int request) {
		this.request = request;
	}
	public Node getNode() {
		return node;
	}

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	public int getK() {
		return k;
	}

	public void setK(int k) {
		this.k = k;
	}

	public Hashtable<Integer, File> getFileList() {
		return fileList;
	}
}