import javax.swing.*;
import javax.swing.border.TitledBorder;
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
    private JButton toggleUsersButton; // Nouveau bouton pour afficher/masquer la liste des utilisateurs
    private JScrollPane userScrollPane; // Pour pouvoir masquer/afficher la liste
    private JSplitPane splitPane; // Pour gérer l'affichage dynamique

    private ChatServer server;
    private ChatClientImpl client;
    private String userName;
    private boolean connected = false;
    private List<String> messageHistory;
    private int historyIndex = -1;
    private boolean usersListVisible = true; // État de visibilité de la liste

    // Couleurs modernes et améliorées
    private final Color BACKGROUND_COLOR = new Color(250, 252, 255);        // Blanc cassé très doux
    private final Color CARD_COLOR = new Color(255, 255, 255);              // Blanc pur
    private final Color PRIMARY_COLOR = new Color(102, 126, 234);           // Lavande profond
    private final Color SUCCESS_COLOR = new Color(67, 160, 71);             // Vert sauge
    private final Color WARNING_COLOR = new Color(255, 167, 38);            // Ambre doux
    private final Color SECONDARY_COLOR = new Color(120, 144, 156);         // Gris ardoise
    private final Color DISCONNECT_COLOR = new Color(229, 115, 115);        // Rose corail
    private final Color BORDER_COLOR = new Color(237, 241, 247);            // Gris perle

    // Nouvelles couleurs pour les messages - palette unique et apaisante
    private final Color USER_MESSAGE_COLOR = new Color(94, 114, 228);       // Indigo doux
    private final Color SYSTEM_MESSAGE_COLOR = new Color(149, 165, 166);    // Gris ardoise clair
    private final Color OWN_MESSAGE_COLOR = new Color(72, 187, 120);        // Émeraude tendre
    private final Color PRIVATE_MESSAGE_COLOR = new Color(171, 71, 188);    // Améthyste
    private final Color TIMESTAMP_COLOR = new Color(155, 164, 181);         // Gris lavande

    // Couleur de fond pour la zone de chat - ton très subtil
    private final Color CHAT_BACKGROUND = new Color(252, 253, 255);

    private int lamportClock = 0;

    public EnhancedChatGUI() {
        messageHistory = new ArrayList<>();
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        applyModernStyling();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("💬 Chat Client");
        setSize(800, 600);
        setLocationRelativeTo(null);
        showConnectionPanel();
    }

    private synchronized void incrementLamportClockOnSend() {
        lamportClock++;
    }

    private void initializeComponents() {
        // Connection components avec amélioration visuelle
        nameField = new JTextField(20);
        nameField.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));

        serverField = new JTextField("localhost", 15);
        serverField.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));

        portSpinner = new JSpinner(new SpinnerNumberModel(1099, 1, 65535, 1));
        portSpinner.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));

        connectButton = new JButton("🔗 Se Connecter");
        connectButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));

        disconnectButton = new JButton("🔌 Se Déconnecter");
        disconnectButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        disconnectButton.setEnabled(false);

        connectionPanel = new JPanel();

        // Chat components avec design amélioré
        chatArea = new JTextPane();
        chatArea.setEditable(false);
        chatArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        chatArea.setBackground(CHAT_BACKGROUND);
        chatArea.setMargin(new Insets(10, 10, 10, 10));

        messageField = new JTextField();
        messageField.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        messageField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        sendButton = new JButton("📤 Envoyer");
        sendButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        sendButton.setEnabled(false);

        // User list avec amélioration
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userList.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        userList.setCellRenderer(new UserListCellRenderer());

        // Nouveau bouton pour toggler la liste des utilisateurs
        toggleUsersButton = new JButton("👥 Masquer Utilisateurs");
        toggleUsersButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));

        // Status and options avec amélioration
        statusLabel = new JLabel("🔴 Déconnecté");
        statusLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));

        soundCheckBox = new JCheckBox("🔊 Notifications sonores", true);
        soundCheckBox.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));

        emojiButton = new JButton("😊");
        emojiButton.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));

        chatPanel = new JPanel();
    }

    private void setupLayout() {
        // Page de connexion améliorée
        connectionPanel.setLayout(new BorderLayout());
        connectionPanel.setBackground(BACKGROUND_COLOR);

        // Panel principal de connexion avec style moderne
        JPanel mainConnectionPanel = new JPanel();
        mainConnectionPanel.setLayout(new BoxLayout(mainConnectionPanel, BoxLayout.Y_AXIS));
        mainConnectionPanel.setBackground(BACKGROUND_COLOR);
        mainConnectionPanel.setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50));

        // Titre
        JLabel titleLabel = new JLabel("💬 Chat RMI - Connexion");
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        titleLabel.setForeground(PRIMARY_COLOR);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 30, 0));

        // Carte de connexion
        JPanel cardPanel = createStyledCard();
        cardPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;

        // Champs de saisie avec labels stylés
        gbc.gridx = 0; gbc.gridy = 0;
        cardPanel.add(createStyledLabel("👤 Nom d'utilisateur:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        cardPanel.add(nameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        cardPanel.add(createStyledLabel("🌐 Serveur:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        cardPanel.add(serverField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        cardPanel.add(createStyledLabel("🔢 Port:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        cardPanel.add(portSpinner, gbc);

        // Boutons de connexion
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(20, 8, 8, 8);
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setBackground(CARD_COLOR);
        buttonPanel.add(connectButton);
        buttonPanel.add(disconnectButton);
        cardPanel.add(buttonPanel, gbc);

        mainConnectionPanel.add(titleLabel);
        mainConnectionPanel.add(cardPanel);
        connectionPanel.add(mainConnectionPanel, BorderLayout.CENTER);

        // Chat panel layout amélioré
        chatPanel.setLayout(new BorderLayout());
        chatPanel.setBackground(BACKGROUND_COLOR);

        // Header avec titre et contrôles
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(CARD_COLOR);
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));

        JLabel chatTitle = new JLabel("💬 Salon de Discussion");
        chatTitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        chatTitle.setForeground(PRIMARY_COLOR);
        headerPanel.add(chatTitle, BorderLayout.WEST);

        JPanel headerControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        headerControls.setBackground(CARD_COLOR);
        headerControls.add(toggleUsersButton);
        headerControls.add(soundCheckBox);
        headerPanel.add(headerControls, BorderLayout.EAST);

        // Zone de chat principale
        JScrollPane chatScrollPane = new JScrollPane(chatArea);
        chatScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        chatScrollPane.setBorder(BorderFactory.createEmptyBorder());
        chatScrollPane.getViewport().setBackground(CHAT_BACKGROUND);

        // Liste des utilisateurs avec style
        userScrollPane = new JScrollPane(userList);
        userScrollPane.setPreferredSize(new Dimension(200, 300));
        userScrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 1, 0, 0, BORDER_COLOR),
                BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10),
                        "👥 Utilisateurs Connectés",
                        TitledBorder.LEFT, TitledBorder.TOP,
                        new Font(Font.SANS_SERIF, Font.BOLD, 12), PRIMARY_COLOR)
        ));

        // Split pane pour chat et utilisateurs
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, chatScrollPane, userScrollPane);
        splitPane.setResizeWeight(0.75);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        splitPane.setDividerSize(1);

        // Panel principal du chat
        JPanel mainChatPanel = new JPanel(new BorderLayout());
        mainChatPanel.setBackground(BACKGROUND_COLOR);
        mainChatPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainChatPanel.add(splitPane, BorderLayout.CENTER);

        // Zone de saisie de message améliorée
        JPanel messagePanel = createStyledCard();
        messagePanel.setLayout(new BorderLayout(10, 0));
        messagePanel.setBorder(BorderFactory.createCompoundBorder(
                messagePanel.getBorder(),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));

        messagePanel.add(messageField, BorderLayout.CENTER);

        JPanel inputButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        inputButtonPanel.setBackground(CARD_COLOR);
        inputButtonPanel.add(emojiButton);
        inputButtonPanel.add(sendButton);
        messagePanel.add(inputButtonPanel, BorderLayout.EAST);

        // Status panel amélioré
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBackground(BACKGROUND_COLOR);
        statusPanel.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        statusPanel.add(statusLabel, BorderLayout.WEST);

        // Combiner le panel de message et le status dans un panel inférieur
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(BACKGROUND_COLOR);
        bottomPanel.add(messagePanel, BorderLayout.CENTER);
        bottomPanel.add(statusPanel, BorderLayout.SOUTH);

        // Assembly final
        chatPanel.add(headerPanel, BorderLayout.NORTH);
        chatPanel.add(mainChatPanel, BorderLayout.CENTER);
        chatPanel.add(bottomPanel, BorderLayout.SOUTH);
    }

    private JPanel createStyledCard() {
        JPanel card = new JPanel();
        card.setBackground(CARD_COLOR);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        return card;
    }

    private JLabel createStyledLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        label.setForeground(new Color(60, 60, 60));
        return label;
    }

    // Renderer personnalisé pour la liste des utilisateurs
    private class UserListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            setText("👤 " + value.toString());
            setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));

            if (isSelected) {
                setBackground(PRIMARY_COLOR);
                setForeground(Color.WHITE);
            } else {
                setBackground(Color.WHITE);
                setForeground(new Color(60, 60, 60));
            }

            setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            return this;
        }
    }

    private void applyModernStyling() {
        // Style des boutons avec nouvelles couleurs professionnelles
        styleButton(connectButton, SUCCESS_COLOR);
        styleButton(disconnectButton, DISCONNECT_COLOR);
        styleButton(sendButton, PRIMARY_COLOR);
        styleButton(toggleUsersButton, SECONDARY_COLOR);

        // Emoji button avec style spécial et UI personnalisé
        emojiButton.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        emojiButton.setBackground(Color.WHITE);
        emojiButton.setForeground(new Color(52, 58, 64));
        emojiButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(206, 212, 218), 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        emojiButton.setFocusPainted(false);
        emojiButton.setContentAreaFilled(true);
        emojiButton.setOpaque(true);
        emojiButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Reste du code inchangé...
        nameField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        serverField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        userList.setBackground(CHAT_BACKGROUND);
        userList.setSelectionBackground(PRIMARY_COLOR);
        userList.setSelectionForeground(Color.WHITE);
    }

    private void styleButton(JButton button, Color color) {
        // Forcer le style personnalisé
        button.setUI(new javax.swing.plaf.basic.BasicButtonUI());

        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        button.setFocusPainted(false);
        button.setContentAreaFilled(true);  // Important pour afficher la couleur
        button.setOpaque(true);            // Force l'opacité
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Effet hover amélioré
        button.addMouseListener(new MouseAdapter() {
            Color originalColor = color;
            Color hoverColor = color.darker();

            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(hoverColor);
                button.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(originalColor);
                button.repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                button.setBackground(hoverColor.darker());
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                button.setBackground(hoverColor);
            }
        });
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
        emojiButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                emojiButton.setBackground(new Color(248, 249, 250));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                emojiButton.setBackground(Color.WHITE);
            }
        });
        // Toggle users button - NOUVEAU
        toggleUsersButton.addActionListener(e -> toggleUsersList());

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

    // NOUVELLE MÉTHODE pour masquer/afficher la liste des utilisateurs
    private void toggleUsersList() {
        if (usersListVisible) {
            splitPane.setRightComponent(null);
            toggleUsersButton.setText("👥 Afficher Utilisateurs");
            usersListVisible = false;
        } else {
            splitPane.setRightComponent(userScrollPane);
            toggleUsersButton.setText("👥 Masquer Utilisateurs");
            usersListVisible = true;
        }
        splitPane.revalidate();
        splitPane.repaint();
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
            showErrorDialog("Veuillez entrer votre nom!");
            return;
        }

        if (serverHost.isEmpty()) {
            showErrorDialog("Veuillez entrer l'adresse du serveur!");
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
            updateStatus("🟢 Connecté à " + serverHost + ":" + port + " en tant que " + userName);

        } catch (Exception e) {
            showErrorDialog("Échec de la connexion au serveur: " + e.getMessage());
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

                updateStatus("🔴 Déconnecté");
                appendSystemMessage("=== Déconnecté du serveur ===");
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
            showErrorDialog("Échec de l'envoi du message: " + e.getMessage());
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
        String[] emojis = {"😊", "😂", "😍", "😢", "😡", "👍", "👎", "❤️", "🎉", "🤔",
                "🔥", "💯", "🎯", "⚡", "🌟", "💡", "🚀", "🎊", "🎈", "🎁"};
        String selectedEmoji = (String) JOptionPane.showInputDialog(
                this, "Choisissez un emoji:", "Emojis",
                JOptionPane.PLAIN_MESSAGE, null, emojis, emojis[0]);

        if (selectedEmoji != null) {
            messageField.setText(messageField.getText() + selectedEmoji);
            messageField.requestFocus();
        }
    }

    public void appendMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            int receivedLamport = 0;
            try {
                if (message.startsWith("[")) {
                    int endIdx = message.indexOf("]");
                    if (endIdx > 1) {
                        String lamportStr = message.substring(1, endIdx);
                        receivedLamport = Integer.parseInt(lamportStr);
                        lamportClock = Math.max(lamportClock, receivedLamport) + 1;
                    }
                }
            } catch (NumberFormatException e) {
            }

            String timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            String formattedMessage = "[" + timestamp + "] " + message;

            try {
                StyledDocument doc = chatArea.getStyledDocument();

                // Style pour le timestamp
                SimpleAttributeSet timestampAttrs = new SimpleAttributeSet();
                StyleConstants.setForeground(timestampAttrs, TIMESTAMP_COLOR);
                StyleConstants.setFontSize(timestampAttrs, 11);
                StyleConstants.setItalic(timestampAttrs, true);

                // Style pour le message
                SimpleAttributeSet messageAttrs = new SimpleAttributeSet();

                if (message.startsWith("===")) {
                    StyleConstants.setForeground(messageAttrs, SYSTEM_MESSAGE_COLOR);
                    StyleConstants.setItalic(messageAttrs, true);
                    StyleConstants.setFontSize(messageAttrs, 12);
                } else if (message.startsWith("[PRIVATE]")) {
                    StyleConstants.setForeground(messageAttrs, PRIVATE_MESSAGE_COLOR);
                    StyleConstants.setBold(messageAttrs, true);
                    StyleConstants.setFontSize(messageAttrs, 13);
                } else if (message.startsWith(userName + ":")) {
                    StyleConstants.setForeground(messageAttrs, OWN_MESSAGE_COLOR);
                    StyleConstants.setBold(messageAttrs, true);
                    StyleConstants.setFontSize(messageAttrs, 13);
                } else {
                    StyleConstants.setForeground(messageAttrs, USER_MESSAGE_COLOR);
                    StyleConstants.setFontSize(messageAttrs, 13);
                }

                // Insérer le timestamp et le message avec leurs styles respectifs
                doc.insertString(doc.getLength(), "[" + timestamp + "] ", timestampAttrs);
                doc.insertString(doc.getLength(), message + "\n", messageAttrs);
                chatArea.setCaretPosition(doc.getLength());

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
        JOptionPane.showMessageDialog(this, message, "Erreur", JOptionPane.ERROR_MESSAGE);
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
                // Style supplémentaire pour les composants Swing
                UIManager.put("Button.font", new Font(Font.SANS_SERIF, Font.BOLD, 12));
                UIManager.put("Label.font", new Font(Font.SANS_SERIF, Font.PLAIN, 12));
            } catch (Exception e) {
                e.printStackTrace();
            }
            new EnhancedChatGUI().setVisible(true);
        });
    }
}