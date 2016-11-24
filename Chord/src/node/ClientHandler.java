package node;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashSet;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

class ClientHandler implements Runnable {
	private int port;
	private HashSet<InetSocketAddress> set;

	public ClientHandler(int port, HashSet<InetSocketAddress> set) {
		this.port = port;
		this.set = set;
	}


	@Override
	public void run() {

	}
}
