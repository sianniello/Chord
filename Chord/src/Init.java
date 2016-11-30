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
				new Thread(nodes[i]).start();
			} catch (ClassNotFoundException | IOException e) {
				e.printStackTrace();
			}
		}
		Scanner s = new Scanner(System.in);
		nodes[0].create();		//node[0] create a Chord ring
		//s.nextLine();
		nodes[0].addFile();		
		nodes[0].addFile();
		s.nextLine();
		nodes[2].join(nodes[0]);
		s.nextLine();
		nodes[2].addFile();
		//s.nextLine();
		//nodes[3].join();
		//nodes[2].addFile();
		s.close();
	}
}

