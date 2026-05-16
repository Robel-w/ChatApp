package client;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
public class ChatGUI extends JFrame{
    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendButton;

    private PrintWriter out;
    private BufferedReader in;
    public ChatGUI (Socket socket) throws Exception{
        //setup networking

        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        //setup UI
        setTitle("Chat App");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        chatArea = new JTextArea();
        chatArea.setEditable(false);

        inputField = new JTextField();
        sendButton = new JButton("Send");

        add(new JScrollPane(chatArea), BorderLayout.CENTER);
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);

        //Send action
        sendButton.addActionListener(e-> sendMessage());
        inputField.addActionListener(e-> sendMessage());

        //start listening to the message
        startMessageReader();
        setVisible(true);
    }
    private void sendMessage(){
        String msg = inputField.getText();

        if (!msg.isEmpty()){
            out.println(msg);
            inputField.setText("");
        }
    }

    private void startMessageReader(){
        new Thread(()->{
            try {
                String msg;
                while ((msg = in.readLine()) != null) {
                    chatArea.append(msg + "\n");
                }
            } catch(Exception e){
                e.printStackTrace();
            }
        }).start();
    }




}
