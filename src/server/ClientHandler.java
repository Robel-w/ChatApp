package server;
import db.ChatDB;

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
                // Save to database (except typing indicators)
                if (!message.startsWith("__TYPING__:")) {
                    // Extract username roughly (you can improve this)
                    String username = message.contains(":") ?
                            message.split(":")[0].replaceAll("\\[.*?\\]\\s*", "") : "Unknown";

                    ChatDB.saveMessage(username, message);
                }

                // Broadcast
                synchronized (ChatServer.clients) {
                    for (PrintWriter client : ChatServer.clients) {
                        client.println(message);
                    }
                }

            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
