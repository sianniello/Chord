package node;

import java.io.File;
import java.io.Serializable;
import java.util.Hashtable;

import randomFile.RandomFile;

@SuppressWarnings("serial")
public class Request implements Serializable{
	
	public static final int addFile_REQ = 1;
	public static final int addFile_RES = 2;
	public static final int addFile = 3;
	public static final int join_REQ = 4;
	public static final int join_RES = 5;
	public static final int start_stabilize = 6;
	public static final int stabilize_request = 7;
	public static final int stabilize = 8;
	public static final int notify = 9;
	public static final int check_alive = 0;
	
	
	private int port;	//destination port
	private int request;	//request type
	private Node node;	//source node
	private File file;
	private int k;

	public Request(int port) {
		this.port = port;
	}
	
	public Request(int port, int request) {
		this.port = port;
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
