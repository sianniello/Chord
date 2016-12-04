import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Scanner;

import javax.crypto.NoSuchPaddingException;

import node.Node;

public class Init {

	public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
		Node[] nodes = new Node[Integer.parseInt(args[0])];
		
		//10 nodes creation
		for(int i = 0; i < Integer.parseInt(args[0]); i++) {
			try {
				nodes[i] = new Node(10000 + i);
				new Thread(nodes[i], "Node[" + (10000 + i) + "]").start();
			} catch (ClassNotFoundException | IOException  e) {
				e.printStackTrace();
			}
		}
		Scanner s = new Scanner(System.in);
		nodes[0].create();		//node[0] create a Chord ring
		nodes[0].addFile();
		s.nextLine();
		nodes[2].joinRing(nodes[0].getPort());
		s.nextLine();
		nodes[0].setOffline();
		s.nextLine();
		nodes[2].addFile();
		s.nextLine();
		nodes[3].joinRing(nodes[0].getPort());
		s.nextLine();
		nodes[4].joinRing(nodes[0].getPort());
		s.nextLine();
		nodes[1].joinRing(nodes[0].getPort());
		s.nextLine();
		nodes[5].joinRing(nodes[0].getPort());
		
		
		s.close();
	}
}
