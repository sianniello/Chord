package node;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

public class NodeTest {

	@Test
	public void testFindSuccessor() throws ClassNotFoundException, IOException {
		Node n = new Node();
		n.setId(5);
		n.setSucc(new Node());
		n.getSucc().setId(2);
		n.getSucc().setSucc(n);
		
		n.addToRing(n);
		n.addToRing(n.getSucc());

		assertEquals(5, n.findSuccessor(4).getId());
		assertEquals(5, n.findSuccessor(5).getId());
		assertEquals(2, n.findSuccessor(6).getId());
		assertEquals(2, n.findSuccessor(7).getId());
//		assertEquals(2, n.findSuccessor(1).getId());

		
	}

}
