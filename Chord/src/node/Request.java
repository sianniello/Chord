package node;

import java.io.File;
import java.io.Serializable;
import java.util.Hashtable;

import randomFile.RandomFile;

@SuppressWarnings("serial")
public class Request implements Serializable{
	
	public static final int add_file = 1;
	public static final int join = 2;
	public static final int stabilize = 3;
	public static final int notify = 4;
	public static final int check = 5;
	public static final int find_successor_ = 6;
	
	private int client_port;
	private int request;
	private Node node;
	private File file;
	private Hashtable<Integer, Node> ring;
	private int k;

	public Request (int request, int k, Node node) {
		this.setK(k);
		this.request = request;
		this.node = node;
	}

	public Request(int addFile, File file) {
		request = addFile;
		this.file = file;
	}

	public Request (int request, Node node) {
		this.request = request;
		this.node = node;
	}

	public Request(int request) {
		this.request = request;
	}

	public Request(int request, Hashtable<Integer, Node> ring, int port) {
		this.request = request;
		this.ring = ring;
		this.client_port = port;
	}

	public Request(int request, File file, Node node) {
		this.request = request;
		this.file = file;
		this.node = node;
	}

	public Request(int addFile, int k, File file) {
		this.request = addFile;
		this.k = k;
	}
	
	public int getClient_port() {
		return client_port;
	}
	public void setClient_port(int client_port) {
		this.client_port = client_port;
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
	public void setData(Node node) {
		this.node = node;
	}

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	@Override
	public String toString() {
		if(file != null)
			return "Filename: " + file.getName() + ", requestId =" + request;
		if(node != null)
			return node.toString() + ", requestId = "; 
		else return "Dummy request";
	}

	public Hashtable<Integer, Node> getRing() {
		return ring;
	}

	public int getK() {
		return k;
	}

	public void setK(int k) {
		this.k = k;
	}
}
