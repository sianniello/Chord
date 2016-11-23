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
public class ClientJS {
    
    private int port; //QUESTA E' LA PORTA DEL SERVER!!!!
    private Socket client;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public ClientJS(int port) throws IOException {
        this.port = port;
        client = new Socket("localhost", port);
        
        out = new ObjectOutputStream(client.getOutputStream());
        in = new ObjectInputStream(client.getInputStream());
    }
    
    public void showSet(HashSet<InetSocketAddress>set){
        
        for(InetSocketAddress address:set)
            System.out.println(address.toString());
    }
    
    public void execute() throws IOException, ClassNotFoundException{
        HashSet<InetSocketAddress> set = null;
        
        InetSocketAddress myAddress = new InetSocketAddress(client.getLocalPort());
        out.writeObject(myAddress);
        
        set = (HashSet<InetSocketAddress>) in.readObject();
        showSet(set);
    }
    
    public static void main(String[] args) throws IOException {
        
        ClientJS clientJS = new ClientJS(1099);
        
        try {
            clientJS.execute();
        } catch (IOException ex) {
            Logger.getLogger(ClientJS.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(ClientJS.class.getName()).log(Level.SEVERE, null, ex);
        }finally{
            if(! clientJS.client.isClosed())
                clientJS.client.close();
        }
    }
    
}
