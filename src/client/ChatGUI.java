package client;

import db.ChatDB;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ChatGUI extends JFrame {
    private Socket socket;
    private JLabel typingLabel;
    private long lastTypingSent = 0;
    private javax.swing.Timer typingStopTimer;
    private String username;
    private JPanel chatPanel;
    private JScrollPane scrollPane;
    private JTextField inputField;
    private JButton sendButton;
    private PrintWriter out;
    private BufferedReader in;

    // Time formatter
    private final DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("hh:mm a");

    public ChatGUI(Socket socket) throws Exception {
        this.socket = socket;
        // NETWORK SETUP
        out = new PrintWriter(socket.getOutputStream(), true);

        in = new BufferedReader(
                new InputStreamReader(socket.getInputStream())
        );

        // Ask username
        username = JOptionPane.showInputDialog(
                this,
                "Enter username:"
        );
        // Send join message
        out.println(username + " joined the chat");

        // UI SETUP
        setTitle("Java Chat App");
        setSize(500, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Chat panel
        chatPanel = new JPanel();
        chatPanel.setLayout(
                new BoxLayout(chatPanel, BoxLayout.Y_AXIS)
        );

        // Dark mode background
        chatPanel.setBackground(new Color(30, 30, 30));

        // Scroll pane
        scrollPane = new JScrollPane(chatPanel);
        scrollPane.setBorder(null);

        add(scrollPane, BorderLayout.CENTER);

        // Bottom input panel
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(new Color(40, 40, 40));

        inputField = new JTextField();
        inputField.setBackground(new Color(50, 50, 50));
        inputField.setForeground(Color.WHITE);
        inputField.setCaretColor(Color.WHITE);

        sendButton = new JButton("Send");

        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);

        typingLabel = new JLabel(" ");
        typingLabel.setForeground(Color.GRAY);

        add(typingLabel, BorderLayout.NORTH);
        // EVENTS
        // Send button
        sendButton.addActionListener(e -> sendMessage());

        // Enter key
        inputField.addActionListener(e -> sendMessage());

        inputField.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyTyped(java.awt.event.KeyEvent e) {

                long now = System.currentTimeMillis();

                if (now - lastTypingSent > 1000) {
                    out.println("__TYPING__:" + username);
                    lastTypingSent = now;
                }
            }
        });

        // Window close event
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                out.println(username + " left the chat");
            }
        });

        // Start listening
        startMessageReader();

        setVisible(true);

        SwingUtilities.invokeLater(() -> {
            ChatDB.loadRecentMessages(this);
        });
    }

    // SEND MESSAGE
    private void sendMessage() {

        String msg = inputField.getText().trim();

        if (!msg.isEmpty()) {

            String time = LocalTime.now().format(formatter);

            out.println("[" + time + "] " + username + ": " + msg);

            inputField.setText("");
            //save to database
            ChatDB.saveMessage(username, msg);
        }
    }
    // For loading old messages from DB
    public void addMessageFromDB(String sender, String message) {
        String fullMsg = message; // you can format it as needed
        boolean isSelf = sender.equals(username);
        addMessage(fullMsg, isSelf);
    }

    // ADD MESSAGE BUBBLE
    private void addMessage(String msg, boolean isSelf) {

        JPanel wrapper = new JPanel(
                new FlowLayout(
                        isSelf ? FlowLayout.RIGHT : FlowLayout.LEFT
                )
        );

        wrapper.setBackground(new Color(30, 30, 30));
        wrapper.setBorder(
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        );

        JLabel messageLabel = new JLabel(msg);

        messageLabel.setOpaque(true);

        messageLabel.setBorder(
                BorderFactory.createEmptyBorder(10, 15, 10, 15)
        );

        // Bubble colors
        if (isSelf) {
            messageLabel.setBackground(new Color(0, 132, 255));
            messageLabel.setForeground(Color.WHITE);
        } else {
            messageLabel.setBackground(new Color(230, 230, 230));
            messageLabel.setForeground(Color.BLACK);
        }

        wrapper.add(messageLabel);

        chatPanel.add(wrapper);

        chatPanel.revalidate();
        chatPanel.repaint();

        // Auto-scroll
        SwingUtilities.invokeLater(() -> {
            JScrollBar vertical =
                    scrollPane.getVerticalScrollBar();

            vertical.setValue(vertical.getMaximum());
        });
    }

    public void addSystemMessage(String msg){
        JPanel wrapper =  new JPanel(new FlowLayout(FlowLayout.CENTER));
        wrapper.setBackground(new Color(30, 30, 30));
        JLabel label = new JLabel(msg);
        label.setForeground(Color.GRAY);
        label.setFont(new Font("Arial", Font.ITALIC, 12));
        wrapper.add(label);
        chatPanel.add(wrapper);
        chatPanel.revalidate();
        chatPanel.repaint();

    }

    private void showTyping(String user) {

        typingLabel.setText(user + " is typing...");

        if (typingStopTimer != null) {
            typingStopTimer.stop();
        }

        typingStopTimer = new javax.swing.Timer(1500, e -> {
            typingLabel.setText(" ");
        });

        typingStopTimer.setRepeats(false);
        typingStopTimer.start();
    }

    // MESSAGE LISTENER THREAD
    private void startMessageReader() {

        new Thread(() -> {

            try {


                String msg;

                while ((msg = in.readLine()) != null) {

                    final String finalMsg = msg;

                    final boolean isSelf =
                            finalMsg.contains(username + ":");

                    if (finalMsg.startsWith("__TYPING__:")) {

                        String typingUser =
                                finalMsg.replace("__TYPING__:", "");

                        if (!typingUser.equals(username)) {

                            SwingUtilities.invokeLater(() -> {
                                showTyping(typingUser);
                            });
                        }

                        continue;
                    }

                    boolean isSystem =
                            finalMsg.contains("joined the chat")
                                    || finalMsg.contains("left the chat");

                    SwingUtilities.invokeLater(() -> {
                        if (isSystem) {
                            addSystemMessage(finalMsg);
                        } else {
                            addMessage(finalMsg, isSelf);
                        }
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }).start();
    }
}