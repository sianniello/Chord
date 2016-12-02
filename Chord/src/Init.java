import java.io.IOException;
import java.util.Scanner;

import node.Node;

public class Init {

	public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
		Node[] nodes = new Node[Integer.parseInt(args[0])];
		
		//10 nodes creation
		for(int i = 0; i < Integer.parseInt(args[0]); i++) {
			try {
				nodes[i] = new Node(10000 + i);
				new Thread(nodes[i], "Node[" + (10000 + i) + "]").start();
			} catch (ClassNotFoundException | IOException e) {
				e.printStackTrace();
			}
		}
		Scanner s = new Scanner(System.in);
		nodes[0].create();		//node[0] create a Chord ring
		nodes[0].saveFile();
		s.nextLine();
		nodes[2].join();
		s.nextLine();
		nodes[0].setOffline();
		s.nextLine();
		nodes[2].saveFile();
		s.nextLine();
		nodes[3].join();
		s.nextLine();
		nodes[4].join();
		s.nextLine();
		nodes[1].join();
		s.nextLine();
		nodes[5].join();
		
		
		s.close();
	}
}

