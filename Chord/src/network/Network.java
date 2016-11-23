package network;

import java.util.HashSet;

import peer.Peer;

public class Network {

	public static void main(String[] args) {
		if(args.length != 0)

			//il ciclo crea 10 peer
			for(int i = 0; i <= 9; i++) {
				new Thread((Runnable) new Peer(10000 + i).start();
			}
	}
}
