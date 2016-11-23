package ring;

import java.io.IOException;

import node.Node;

public class Ring {

	public static void main(String[] args) {
		if(args.length != 0)

			//il ciclo crea 10 peer
			for(int i = 0; i <= 9; i++) {
				try {
					new Thread(new Node(10000 + i)).start();
				} catch (ClassNotFoundException | IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
	}
}
