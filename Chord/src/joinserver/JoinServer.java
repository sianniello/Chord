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
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author daniele
 */
public class JoinServer {
    
    private int port;
    private ServerSocket server;
    private HashSet<InetSocketAddress> set;

    public JoinServer(int port) throws IOException {
        this.port = port;
        server = new ServerSocket(port);
        set = new HashSet<>();
        System.out.println("Server listening at: " + port);
    }
    
    public void execute() throws IOException{
        
        Socket client =null;
        
        Executor executor = Executors.newFixedThreadPool(1000);
        
        while(true){
            client = server.accept();
            executor.execute(new ServerHandler(client, set));
        }
     
    }
    
    public static void main(String[] args) throws IOException {
        
        JoinServer jserver = new JoinServer(1099);
        
        try {
            jserver.execute();
        } catch (IOException ex) {
            Logger.getLogger(JoinServer.class.getName()).log(Level.SEVERE, null, ex);
        }finally{
            if(! jserver.server.isClosed()) 
                jserver.server.close();
        }
    }
    
    
}
