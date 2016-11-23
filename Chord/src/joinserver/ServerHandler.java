/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joinserver;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author daniele
 */
class ServerHandler implements Runnable {
     private Socket client;
     private HashSet<InetSocketAddress> set;
     private ObjectOutputStream out;
     private ObjectInputStream in;
     
     
    public ServerHandler(Socket client, HashSet<InetSocketAddress> set) throws IOException {
        this.client = client;
        this.set = set;
        out = new ObjectOutputStream(client.getOutputStream());
        in = new ObjectInputStream(client.getInputStream());
        
    }

    @Override
    public void run() {
    
         try {
             InetSocketAddress address = (InetSocketAddress) in.readObject();
             System.out.println("Server " + Thread.currentThread().getName() + " ricevuto: " + address.toString()); 
             
             synchronized(this){
                 set.add(address);
             }
             
             out.writeObject(set);
             
         } catch (IOException ex) {
             Logger.getLogger(ServerHandler.class.getName()).log(Level.SEVERE, null, ex);
         } catch (ClassNotFoundException ex) {
             Logger.getLogger(ServerHandler.class.getName()).log(Level.SEVERE, null, ex);
         }
    }
    
}
