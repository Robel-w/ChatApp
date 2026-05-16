package server;
import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public ClientHandler(Socket socket) throws Exception{
        this.socket = socket;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out  = new PrintWriter(socket.getOutputStream(), true);
        synchronized (ChatServer.clients){
            ChatServer.clients.add(out);
        }

    }
    @Override
    public void run(){
        try{
            String message;
            while ((message = in.readLine()) != null){
                System.out.println("Client: " + message);
                for (PrintWriter client : ChatServer.clients) {
                    client.println(message);
                }

            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
