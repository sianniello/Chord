import java.io.IOException;
import node.Node;

public class Init {

	public static void main(String[] args) throws IOException, ClassNotFoundException {
		Node[] nodes = new Node[Integer.parseInt(args[0])];
		//10 nodes creation
		for(int i = 0; i < Integer.parseInt(args[0]); i++) {

			try {
				nodes[i] = new Node(10000 + i);
				new Thread(nodes[i]).start();
			} catch (ClassNotFoundException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		nodes[0].create();
		nodes[0].addFile();
		nodes[0].addFile();
		//nodes[3].join();
		//nodes[3].addFile();
		//nodes[1].join();
		//nodes[2].join();
	}
}

