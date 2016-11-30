package node;

import java.io.File;
import java.io.Serializable;
import java.util.Hashtable;

import randomFile.RandomFile;

@SuppressWarnings("serial")
public class Request implements Serializable{
	
	public static final int save_file = 0;
	public static final int add_file = 1;
	public static final int join = 2;
	public static final int stabilize = 3;
	public static final int notify = 4;
	public static final int check = 5;
	public static final int find_successor_ = 6;
	public static final int join_request = 7;
	public static final int stabilize_request = 8;
	
	private int port;
	private int request;
	private Node node;
	private File file;
	private int k;

	public Request(int port) {
		this.port = port;
	}
	
	public Request(int port, int request) {
		this(port);
		this.request = request;
	}
	
	public Request(int port, int request, File file) {
		this(port, request);
		this.file = file;
	}
	
	public Request(int port, int request, Node node) {
		this(port, request);
		this.node = node;
	}
	
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
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

	@Override
	public String toString() {
		if(file != null)
			return "Filename: " + file.getName() + ", requestId =" + request;
		if(node != null)
			return node.toString() + ", requestId = "; 
		else return "Dummy request";
	}

	public int getK() {
		return k;
	}

	public void setK(int k) {
		this.k = k;
	}
}
