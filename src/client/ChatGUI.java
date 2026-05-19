package client;

import db.ChatDB;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

public class ChatGUI extends JFrame {
    private java.util.Set<String> recentlySentFiles = new java.util.HashSet<>();

    private Socket socket;
    private String username;

    private PrintWriter out;
    private BufferedReader in;

    private JPanel chatPanel;
    private JScrollPane scrollPane;
    private JTextField inputField;
    private JButton sendButton;
    private JButton fileButton;

    private JLabel typingLabel;
    private long lastTypingSent = 0;
    private javax.swing.Timer typingStopTimer;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a");

    public ChatGUI(Socket socket) throws Exception {
        this.socket = socket;

        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        username = JOptionPane.showInputDialog(this, "Enter username:");
        if (username == null || username.trim().isEmpty()) username = "User" + (System.currentTimeMillis() % 10000);

        out.println("SYSTEM|" + username + " joined the chat");

        // UI
        setTitle("Chat - " + username);
        setSize(650, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        chatPanel = new JPanel();
        chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));
        chatPanel.setBackground(new Color(30, 30, 30));

        scrollPane = new JScrollPane(chatPanel);
        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.CENTER);

        // Bottom Panel
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(new Color(40, 40, 40));

        inputField = new JTextField();
        inputField.setBackground(new Color(50, 50, 50));
        inputField.setForeground(Color.WHITE);
        inputField.setCaretColor(Color.WHITE);

        sendButton = new JButton("Send");
        fileButton = new JButton("📎");

        JPanel inputArea = new JPanel(new BorderLayout());
        inputArea.add(fileButton, BorderLayout.WEST);
        inputArea.add(inputField, BorderLayout.CENTER);
        inputArea.add(sendButton, BorderLayout.EAST);

        bottomPanel.add(inputArea);
        add(bottomPanel, BorderLayout.SOUTH);

        typingLabel = new JLabel(" ");
        typingLabel.setForeground(Color.GRAY);
        add(typingLabel, BorderLayout.NORTH);

        // Listeners
        fileButton.addActionListener(e -> sendFile());
        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());

        inputField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent e) {
                long now = System.currentTimeMillis();
                if (now - lastTypingSent > 1000) {
                    out.println("TYPING|" + username);
                    lastTypingSent = now;
                }
            }
        });

        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent e) {
                out.println("SYSTEM|" + username + " left the chat");
            }
        });

        startMessageReader();
        setVisible(true);

        SwingUtilities.invokeLater(() -> ChatDB.loadRecentMessages(this));
    }

    // sending file
    private void sendFile() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();

            if (file.length() > 8 * 1024 * 1024) {
                JOptionPane.showMessageDialog(this, "Max file size: 8MB", "Too Large", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                byte[] bytes = Files.readAllBytes(file.toPath());
                String base64 = Base64.getEncoder().encodeToString(bytes);

                String filename = file.getName();
                // Mark as recently sent
                recentlySentFiles.add(filename);
                // After sending...
                out.println("FILE|" + file.getName() + "|" + base64);

// Show locally
                SwingUtilities.invokeLater(() -> receiveFile(file.getName(), base64, true));

// ✅ Save to Database
                ChatDB.saveFile(username, file.getName(), base64);

                // Remove from recent after 3 seconds
                new Timer(3000, e -> recentlySentFiles.remove(filename)).start();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void sendMessage() {
        String text = inputField.getText().trim();
        if (!text.isEmpty()) {
            String time = LocalTime.now().format(formatter);
            String fullMsg = "[" + time + "] " + username + ": " + text;

            out.println("CHAT|" + fullMsg);
            inputField.setText("");

            ChatDB.saveMessage(username, text);
        }
    }

    // receive file
    private void receiveFile(String filename, String base64Data, boolean isSelf) {
        try {
            byte[] data = Base64.getDecoder().decode(base64Data);
            String lower = filename.toLowerCase();

            JPanel wrapper = new JPanel(new FlowLayout(isSelf ? FlowLayout.RIGHT : FlowLayout.LEFT));
            wrapper.setBackground(new Color(30, 30, 30));
            wrapper.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

            if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".gif")) {
                // Image
                ImageIcon orig = new ImageIcon(data);
                Image scaled = orig.getImage().getScaledInstance(220, -1, Image.SCALE_SMOOTH);
                JLabel imgLabel = new JLabel(new ImageIcon(scaled));
                imgLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));

                imgLabel.addMouseListener(new MouseAdapter() {
                    public void mouseClicked(MouseEvent e) {
                        showFullImage(orig, filename);
                    }
                });

                wrapper.add(imgLabel);
            } else {
                // fif it is other file
                JButton btn = new JButton("📎 " + filename);
                btn.setBackground(new Color(70, 70, 70));
                btn.setForeground(Color.WHITE);
                btn.addActionListener(e -> saveAndOpenFile(filename, data));
                wrapper.add(btn);
            }

            chatPanel.add(wrapper);
            chatPanel.revalidate();
            chatPanel.repaint();

            SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getMaximum()));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showFullImage(ImageIcon icon, String title) {
        JDialog dialog = new JDialog(this, title, true);
        dialog.add(new JScrollPane(new JLabel(icon)));
        dialog.setSize(900, 700);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void saveAndOpenFile(String filename, byte[] data) {
        try {
            File dir = new File("Downloads");
            dir.mkdirs();
            File file = new File(dir, filename);
            Files.write(file.toPath(), data);

            JOptionPane.showMessageDialog(this, "Saved: " + file.getAbsolutePath());
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(file);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // the normal message
    private void addMessage(String msg, boolean isSelf) {
        JPanel wrapper = new JPanel(new FlowLayout(isSelf ? FlowLayout.RIGHT : FlowLayout.LEFT));
        wrapper.setBackground(new Color(30, 30, 30));

        JLabel label = new JLabel(msg);
        label.setOpaque(true);
        label.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        if (isSelf) {
            label.setBackground(new Color(0, 123, 255));
            label.setForeground(Color.WHITE);
        } else {
            label.setBackground(new Color(230, 230, 230));
            label.setForeground(Color.BLACK);
        }

        wrapper.add(label);
        chatPanel.add(wrapper);
        chatPanel.revalidate();
        chatPanel.repaint();
    }

    public void addFileFromDB(String sender, String filename, String base64Data) {
        boolean isSelf = sender.equals(username);
        SwingUtilities.invokeLater(() -> {
            receiveFile(filename, base64Data, isSelf);
        });
    }

    public void addMessageFromDB(String sender, String message) {
        addMessage(message, sender.equals(username));
    }

    public void addSystemMessage(String msg) {
        JPanel w = new JPanel(new FlowLayout(FlowLayout.CENTER));
        w.setBackground(new Color(30, 30, 30));
        JLabel l = new JLabel(msg);
        l.setForeground(Color.GRAY);
        w.add(l);
        chatPanel.add(w);
        chatPanel.revalidate();
        chatPanel.repaint();
    }

    private void showTyping(String user) {
        typingLabel.setText(user + " is typing...");
        if (typingStopTimer != null) typingStopTimer.stop();
        typingStopTimer = new Timer(1500, e -> typingLabel.setText(" "));
        typingStopTimer.setRepeats(false);
        typingStopTimer.start();
    }

    // the main listener
    private void startMessageReader() {
        new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    final String msg = line;
                    String[] parts = msg.split("\\|", 2);

                    if (parts.length < 2) continue;

                    String type = parts[0];
                    String content = parts[1];

                    switch (type) {
                        case "CHAT":
                            boolean isSelfChat = content.contains(username + ":");
                            SwingUtilities.invokeLater(() -> addMessage(content, isSelfChat));
                            break;

                        case "SYSTEM":
                            SwingUtilities.invokeLater(() -> addSystemMessage(content));
                            break;

                        case "TYPING":
                            if (!content.equals(username)) {
                                SwingUtilities.invokeLater(() -> showTyping(content));
                            }
                            break;

                        case "FILE":
                            String[] fParts = content.split("\\|", 2);
                            if (fParts.length == 2) {
                                String filename = fParts[0];
                                String base64 = fParts[1];
                                SwingUtilities.invokeLater(() -> {
                                    if (!recentlySentFiles.contains(filename)) {
                                        receiveFile(filename, base64, false);
                                    }
                                });
                            }
                            break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}