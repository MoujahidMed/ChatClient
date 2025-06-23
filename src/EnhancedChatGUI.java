import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class EnhancedChatGUI extends JFrame {
    private JTextPane chatArea;
    private JTextField messageField;
    private JButton sendButton;
    private JTextField nameField;
    private JTextField serverField;
    private JSpinner portSpinner;
    private JButton connectButton;
    private JButton disconnectButton;
    private JPanel connectionPanel;
    private JPanel chatPanel;
    private JLabel statusLabel;
    private JList<String> userList;
    private DefaultListModel<String> userListModel;
    private JCheckBox soundCheckBox;
    private JButton emojiButton;

    private ChatServer server;
    private ChatClientImpl client;
    private String userName;
    private boolean connected = false;
    private List<String> messageHistory;
    private int historyIndex = -1;

    // Colors for different message types
    private final Color USER_MESSAGE_COLOR = new Color(0, 100, 0);
    private final Color SYSTEM_MESSAGE_COLOR = new Color(150, 150, 150);
    private final Color OWN_MESSAGE_COLOR = new Color(0, 0, 150);
    private final Color PRIVATE_MESSAGE_COLOR = new Color(150, 0, 150);

    private int lamportClock = 0;

    public EnhancedChatGUI() {
        messageHistory = new ArrayList<>();
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("Enhanced RMI Chat Client v2.0");
        setSize(700, 500);
        setLocationRelativeTo(null);
        showConnectionPanel();
    }

    private synchronized void incrementLamportClockOnSend() {
        lamportClock++;
    }

    private void initializeComponents() {
        // Connection components
        nameField = new JTextField(15);
        serverField = new JTextField("localhost", 10);
        portSpinner = new JSpinner(new SpinnerNumberModel(1099, 1, 65535, 1));
        connectButton = new JButton("Connect");
        disconnectButton = new JButton("Disconnect");
        disconnectButton.setEnabled(false);
        connectionPanel = new JPanel();

        // Chat components
        chatArea = new JTextPane();
        chatArea.setEditable(false);
        chatArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));

        messageField = new JTextField();
        sendButton = new JButton("Send");
        sendButton.setEnabled(false);

        // User list
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userList.setBorder(BorderFactory.createTitledBorder("Online Users"));

        // Status and options
        statusLabel = new JLabel("Disconnected");
        soundCheckBox = new JCheckBox("Sound notifications", true);
        emojiButton = new JButton("😊");

        chatPanel = new JPanel();
    }

    private void setupLayout() {
        // Connection panel layout
        connectionPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridx = 0; gbc.gridy = 0;
        connectionPanel.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1;
        connectionPanel.add(nameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        connectionPanel.add(new JLabel("Server:"), gbc);
        gbc.gridx = 1;
        connectionPanel.add(serverField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        connectionPanel.add(new JLabel("Port:"), gbc);
        gbc.gridx = 1;
        connectionPanel.add(portSpinner, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(connectButton);
        buttonPanel.add(disconnectButton);
        connectionPanel.add(buttonPanel, gbc);

        // Chat panel layout
        chatPanel.setLayout(new BorderLayout());

        // Main chat area
        JScrollPane chatScrollPane = new JScrollPane(chatArea);
        chatScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        chatScrollPane.setPreferredSize(new Dimension(400, 300));

        // User list
        JScrollPane userScrollPane = new JScrollPane(userList);
        userScrollPane.setPreferredSize(new Dimension(150, 300));

        // Split pane for chat and users
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, chatScrollPane, userScrollPane);
        splitPane.setResizeWeight(0.75);
        chatPanel.add(splitPane, BorderLayout.CENTER);

        // Message input panel
        JPanel messagePanel = new JPanel(new BorderLayout());
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(messageField, BorderLayout.CENTER);

        JPanel buttonPanel2 = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));
        buttonPanel2.add(emojiButton);
        buttonPanel2.add(sendButton);
        inputPanel.add(buttonPanel2, BorderLayout.EAST);

        messagePanel.add(inputPanel, BorderLayout.CENTER);
        chatPanel.add(messagePanel, BorderLayout.SOUTH);

        // Status panel
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.add(statusLabel, BorderLayout.WEST);
        statusPanel.add(soundCheckBox, BorderLayout.EAST);
        chatPanel.add(statusPanel, BorderLayout.NORTH);
    }

    private void setupEventHandlers() {
        // Connect button
        connectButton.addActionListener(e -> connectToServer());

        // Disconnect button
        disconnectButton.addActionListener(e -> disconnectFromServer());

        // Send button
        sendButton.addActionListener(e -> sendMessage());

        // Emoji button
        emojiButton.addActionListener(e -> showEmojiDialog());

        // Enter key in name field
        nameField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && !connected) {
                    connectToServer();
                }
            }
        });

        // Message field key handling
        messageField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendMessage();
                } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                    navigateHistory(true);
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    navigateHistory(false);
                }
            }
        });

        // Window closing event
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (connected) {
                    disconnectFromServer();
                }
            }
        });

        // Double-click on user list for private message
        userList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String selectedUser = userList.getSelectedValue();
                    if (selectedUser != null && !selectedUser.equals(userName)) {
                        messageField.setText("@" + selectedUser + " ");
                        messageField.requestFocus();
                    }
                }
            }
        });
    }

    private void showConnectionPanel() {
        getContentPane().removeAll();
        add(connectionPanel, BorderLayout.CENTER);
        revalidate();
        repaint();
        nameField.requestFocus();
    }

    private void showChatPanel() {
        getContentPane().removeAll();
        add(chatPanel, BorderLayout.CENTER);
        revalidate();
        repaint();
        messageField.requestFocus();
    }

    private void connectToServer() {
        String name = nameField.getText().trim();
        String serverHost = serverField.getText().trim();
        int port = (Integer) portSpinner.getValue();

        if (name.isEmpty()) {
            showErrorDialog("Please enter your name!");
            return;
        }

        if (serverHost.isEmpty()) {
            showErrorDialog("Please enter server address!");
            return;
        }

        try {
            userName = name;
            Registry registry = LocateRegistry.getRegistry(serverHost, port);
            server = (ChatServer) registry.lookup("ChatServer");
            client = new ChatClientImpl(this);
            server.registerClient(userName, client);

            connected = true;
            connectButton.setEnabled(false);
            disconnectButton.setEnabled(true);
            sendButton.setEnabled(true);

            showChatPanel();
            updateStatus("Connected to " + serverHost + ":" + port + " as " + userName);

        } catch (Exception e) {
            showErrorDialog("Failed to connect to server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void disconnectFromServer() {
        if (connected) {
            try {
                server.unregisterClient(userName);
                connected = false;
                connectButton.setEnabled(true);
                disconnectButton.setEnabled(false);
                sendButton.setEnabled(false);

                updateStatus("Disconnected");
                appendSystemMessage("=== Disconnected from server ===");
                userListModel.clear();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void appendSystemMessage(String s) {
        System.out.println("message");
    }

    private void sendMessage() {
        if (!connected) return;

        String message = messageField.getText().trim();
        if (message.isEmpty()) return;

        try {
            incrementLamportClockOnSend();

            // Add to history
            messageHistory.add(message);
            if (messageHistory.size() > 50) { // Keep last 50 messages
                messageHistory.remove(0);
            }
            historyIndex = -1;

            server.sendMessage(userName, message, lamportClock);
            messageField.setText("");

        } catch (Exception e) {
            showErrorDialog("Failed to send message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void navigateHistory(boolean up) {
        if (messageHistory.isEmpty()) return;

        if (up) {
            if (historyIndex == -1) {
                historyIndex = messageHistory.size() - 1;
            } else if (historyIndex > 0) {
                historyIndex--;
            }
        } else {
            if (historyIndex != -1 && historyIndex < messageHistory.size() - 1) {
                historyIndex++;
            } else {
                historyIndex = -1;
                messageField.setText("");
                return;
            }
        }

        if (historyIndex >= 0 && historyIndex < messageHistory.size()) {
            messageField.setText(messageHistory.get(historyIndex));
        }
    }

    private void showEmojiDialog() {
        String[] emojis = {"😊", "😂", "😍", "😢", "😡", "👍", "👎", "❤️", "🎉", "🤔"};
        String selectedEmoji = (String) JOptionPane.showInputDialog(
                this, "Choose an emoji:", "Emojis",
                JOptionPane.PLAIN_MESSAGE, null, emojis, emojis[0]);

        if (selectedEmoji != null) {
            messageField.setText(messageField.getText() + selectedEmoji);
            messageField.requestFocus();
        }
    }

    public void appendMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            // Extract Lamport timestamp from message prefix, e.g. [12]
            int receivedLamport = 0;
            try {
                if (message.startsWith("[")) {
                    int endIdx = message.indexOf("]");
                    if (endIdx > 1) {
                        String lamportStr = message.substring(1, endIdx);
                        receivedLamport = Integer.parseInt(lamportStr);
                    }
                }
            } catch (NumberFormatException e) {
                // Ignore if format is unexpected
            }

            String timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            String formattedMessage = "[" + timestamp + "] " + message;

            try {
                StyledDocument doc = chatArea.getStyledDocument();
                SimpleAttributeSet attrs = new SimpleAttributeSet();

                // Color coding based on message type
                if (message.startsWith("===")) {
                    StyleConstants.setForeground(attrs, SYSTEM_MESSAGE_COLOR);
                    StyleConstants.setItalic(attrs, true);
                } else if (message.startsWith("[PRIVATE]")) {
                    StyleConstants.setForeground(attrs, PRIVATE_MESSAGE_COLOR);
                    StyleConstants.setBold(attrs, true);
                } else if (message.startsWith(userName + ":")) {
                    StyleConstants.setForeground(attrs, OWN_MESSAGE_COLOR);
                    StyleConstants.setBold(attrs, true);
                } else {
                    StyleConstants.setForeground(attrs, USER_MESSAGE_COLOR);
                }

                doc.insertString(doc.getLength(), formattedMessage + "\n", attrs);
                chatArea.setCaretPosition(doc.getLength());

                // Play sound notification
                if (soundCheckBox.isSelected() && !message.startsWith(userName + ":")) {
                    playNotificationSound();
                }

            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }

    public void updateUserList(List<String> users) {
        SwingUtilities.invokeLater(() -> {
            userListModel.clear();
            for (String user : users) {
                userListModel.addElement(user);
            }
        });
    }

    private void updateStatus(String status) {
        statusLabel.setText(status);
    }

    private void showErrorDialog(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void playNotificationSound() {
        try {
            Toolkit.getDefaultToolkit().beep();
        } catch (Exception e) {
            // Ignore if sound fails
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new EnhancedChatGUI().setVisible(true);
        });
    }
}