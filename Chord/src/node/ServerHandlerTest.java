package node;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.UnknownHostException;

import org.junit.Test;

public class ServerHandlerTest {

	@Test
	public void testSuccessor() throws UnknownHostException, ClassNotFoundException, IOException {
		ServerHandler sh = new ServerHandler();
		sh.m = 8;
		
		assertTrue(sh.successor(6,3,5));
		assertFalse(sh.successor(6,3,6));
		assertFalse(sh.successor(6,3,7));
		assertFalse(sh.successor(6,3,0));
		assertFalse(sh.successor(6,3,1));
		assertFalse(sh.successor(6,3,2));
		assertFalse(sh.successor(6,3,3));
		assertTrue(sh.successor(6,3,4));
		
		assertFalse(sh.successor(3,6,3));
		assertTrue(sh.successor(3,6,2));
		assertFalse(sh.successor(3,6,3));
		assertFalse(sh.successor(3,6,4));
		assertFalse(sh.successor(3,6,5));
		assertFalse(sh.successor(3,6,6));
		assertTrue(sh.successor(3,6,7));
		assertTrue(sh.successor(3,6,0));
	}

}
