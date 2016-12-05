import java.io.IOException;
import java.util.Scanner;

import node.Node;

public class Init {
	
	public static final int num_nodes = 10;

	public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
		Node[] nodes = new Node[num_nodes];
		
		for(int i = 0; i < num_nodes; i++) {
			try {
				nodes[i] = new Node(10000 + i);
				new Thread(nodes[i], "Node[" + (10000 + i) + "]").start();
			} catch (ClassNotFoundException | IOException  e) {
				e.printStackTrace();
			}
		}
		Scanner s = new Scanner(System.in);
		nodes[0].create();		
		nodes[0].addFile();
		nodes[2].joinRing(1);
		nodes[3].joinRing(1);
		nodes[4].joinRing(1);
		nodes[1].joinRing(1);
		nodes[5].joinRing(1);
		
		s.close();
	}
}
