package node;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;

@SuppressWarnings("serial")
public class Request implements Serializable{
	private int client_port;
	private int request;
	private Node node;
	private File file;

	public Request (int client_port, int request, Node node) {
		this.client_port = client_port;
		this.request = request;
		this.node = node;
	}

	public Request(int port, int addFile, File file) {
		client_port = port;
		request = addFile;
		this.file = file;
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

	public String toString() {
		return client_port + "|" + request + "|" + (file != null ? file.getName() : node.toString());
	}
}
