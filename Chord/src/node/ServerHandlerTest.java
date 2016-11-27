package node;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import org.junit.Test;

public class ServerHandlerTest {

	@Test
	public void testFindSuccessor() throws UnknownHostException, ClassNotFoundException, IOException {
		Node node1 = new Node();
		node1.setId(6);
		ServerHandler tester = new ServerHandler(node1);
		node1.setSucc(node1);
		node1.setPred(null);
		Node node2 = new Node();
		node2.setId(3);
		node2.setPred(null);
		
		assertEquals(6, tester.findSuccessor(node2).getId());
		node1.setSucc(node2);
		
		Node node3 = new Node();
		node3.setId(7);
		node2.setPred(null);
		
		assertEquals(3, tester.findSuccessor(node3).getId());
	}

}
