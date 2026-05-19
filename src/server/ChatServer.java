package server;
import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    public static List<PrintWriter> clients =
            Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) throws Exception {
        ServerSocket serverSocket = new ServerSocket(5000);
        System.out.println("Server started ...");

        while(true){
            Socket socket = serverSocket.accept();
            System.out.println("New Client connected!");

            ClientHandler handler = new ClientHandler(socket);
            new Thread(handler).start();

        }
//        Socket socket = serverSocket.accept();
//        System.out.println("Client connected!");
//
//        BufferedReader in = new BufferedReader(
//                new InputStreamReader(socket.getInputStream()));
//        String message;
//        while((message = in.readLine()) != null){
//            System.out.println("client: " + message);
//        }

    }
}
