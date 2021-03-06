package sk.tuke.ds.chat.layouts;

import sk.tuke.ds.chat.messaging.Message;
import sk.tuke.ds.chat.messaging.PrivateMessage;
import sk.tuke.ds.chat.node.NodeId;
import sk.tuke.ds.chat.rmi.ChatNodeServer;
import sk.tuke.ds.chat.rmi.abstraction.AbstractProcess;
import sk.tuke.ds.chat.util.ChatSettings;
import sk.tuke.ds.chat.util.Log;
import sk.tuke.ds.chat.util.Util;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ChatLayout {
    private JTabbedPane tabbedPane;
    private JPanel mainPanel;
    private JTextField peerNodeIpTextField;
    private JTextField peerNodePortTextField;
    private JButton connectButton;
    private JButton hostButton;
    private JTextField clientHostPortTextField;
    private JButton disconnectButton;
    private JTextField usernameTextField;
    private JButton renameUserButton;
    private JPanel contentPanel;
    private JTextPane messagesTextPane;
    private JList usersList;
    private JTextArea messageTextArea;
    private JButton sendButton;
    private JPanel templateTab;
    private JPanel userSettingsPanel;
    private JPanel statusPanel;
    private JButton addPeerButton;
    private JTextField addPeerTextField;
    private JCheckBox useUPnPCheckBox;
    private JCheckBox removeDeadPeersCheckBox;
    private static JLabel staticStatus;
    private static String staticStatusMessage = null;

    public ChatLayout() {
        // Manually set names for the objects that are gonna be looked up (including their cloned versions)
        this.messageTextArea.setName("messageTextArea");
        this.messagesTextPane.setName("messagesTextPane");
        this.usernameTextField.setName("usernameTextField");
        this.renameUserButton.setName("renameUserButton");
        this.disconnectButton.setName("disconnectButton");
        this.usersList.setName("usersList");
        this.sendButton.setName("sendButton");
        this.addPeerButton.setName("addPeerButton");
        this.addPeerTextField.setName("addPeerTextField");
        this.useUPnPCheckBox.setName("useUPnPCheckBox");
        staticStatus = ((JLabel) statusPanel.getComponent(0));

        // Hacky workaround to get Status working from any context
        new AbstractProcess() {
            @Override
            public void run() {
                while (isRunning()) {
                    if (staticStatusMessage != null) {
                        staticStatus.setText("Status: " + staticStatusMessage);
                        staticStatusMessage = null;
                    }
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();

        // Configuration listeners
        this.connectButton.addActionListener(actionEvent -> {
            // Connect to IP
            try {
                int hostPort = Integer.parseInt(this.clientHostPortTextField.getText());
                ArrayList<String> peers = new ArrayList<>();
                // Chosen peer ID
                peers.add(new NodeId(
                        Integer.parseInt(this.peerNodePortTextField.getText()),
                        this.peerNodeIpTextField.getText(),
                        "?"
                ).getNodeIdString());
                // Persisted other known peers
                peers.addAll(Util.loadPeersConfiguration(hostPort));
                // Connection
                createTab(new ChatNodeServer(
                        hostPort,
                        peers,
                        this.useUPnPCheckBox.isSelected()
                ));
            } catch (RemoteException | UnknownHostException e) {
                e.printStackTrace();
                throw new RuntimeException("Could not create server", e);
            }
        });
        this.hostButton.addActionListener(actionEvent -> {
            // Host new server
            try {
                createTab(new ChatNodeServer(
                        Integer.parseInt(this.clientHostPortTextField.getText()),
                        new ArrayList<>(),
                        this.useUPnPCheckBox.isSelected()
                ));
            } catch (RemoteException | UnknownHostException e) {
                e.printStackTrace();
                throw new RuntimeException("Could not create server", e);
            }
        });
        removeDeadPeersCheckBox.addActionListener(actionEvent -> {
            ChatSettings.isRemoveDeadPeers = removeDeadPeersCheckBox.isSelected();
            Log.i(this, "Changed preference of dead peers removal");
        });

        // Example template tab is also used to test the listener generation on (Optional for testing)
        generateListeners(this.templateTab);

        // Hide the template though
        this.tabbedPane.remove(this.templateTab);

        // Space for auto-generated listeners, don't forget to move them to generateListeners method
    }

    public static void setStatus(String status) {
        staticStatusMessage = status;
    }

    private JPanel createTab(ChatNodeServer chatNodeServer) {
        JPanel tabPanel = generateListeners(Util.copySerializable(this.templateTab));
        ChatTab chatTab = ChatTab.lookup(tabPanel);
        chatTab.setServer(chatNodeServer);
        this.tabbedPane.addTab(
                generateTabTitle(chatTab),
                tabPanel
        );
        this.tabbedPane.setSelectedComponent(tabPanel);
        chatTab.generateUsername();

        // Transferring focus to message text area
        //Util.findComponentIn(tabPanel, "messageTextArea").requestFocusInWindow();
        //Util.findComponentIn(tabPanel, "usernameTextField").setEnabled(false);
        //Util.findComponentIn(tabPanel, "usernameTextField").setEnabled(true);

        updateTabTitle(chatTab);
        return tabPanel;
    }

    private void updateTabTitle(ChatTab chatTab) {
        // Update assuming it's current tab
        this.tabbedPane.setTitleAt(
                this.tabbedPane.getSelectedIndex(),
                generateTabTitle(chatTab)
        );
    }

    private String generateTabTitle(ChatTab chatTab) {
        NodeId thisServerNodeId = chatTab.getServer().getNodeId();
        return thisServerNodeId.getUsername()
                + "@"
                + thisServerNodeId.getHostAddress()
                + ":"
                + thisServerNodeId.getPort();
    }

    @Deprecated
    private String generateTabTitleAsTargetPeer(ChatTab chatTab, String username) {
        // Get first peer node id
        List<String> peers = new ArrayList<>(chatTab.getServer().getContext().getPeersCopy());
        String peerNodeIdString = peers.size() > 0 ? peers.get(0) : null;
        NodeId peerNodeId = new NodeId(peerNodeIdString);
        // Get this node id too
        NodeId thisServerNodeId = chatTab.getServer().getNodeId();
        return username
                + "@"
                + (peerNodeIdString == null ? "localhost" : peerNodeId.getHostAddress())
                + ":"
                + (peerNodeIdString == null ? thisServerNodeId.getPort() : peerNodeId.getPort());
    }

    private <T extends JPanel> T generateListeners(T tabPanel) {
        ChatTab chatTab = ChatTab.lookup(tabPanel);
        // Tab configuration
        Util.<JButton>findComponentIn(tabPanel, "renameUserButton").addActionListener(e -> {
            Log.d(this, "Renaming user");
            String username = Util.<JTextField>findComponentIn(tabPanel, "usernameTextField").getText();
            if ("".equals(username.trim())) {
                Log.e(this, "No username provided");
                return;
            }
            chatTab.setUsername(username);
            updateTabTitle(chatTab);
        });
        Util.<JButton>findComponentIn(tabPanel, "disconnectButton").addActionListener(e -> {
            Log.d(this, "Disconnected user");
            this.tabbedPane.remove(tabPanel);
            ChatTab.lookup(tabPanel).onDisconnect();
        });

        // Sending messages
        Util.findComponentIn(tabPanel, "messageTextArea").addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                super.keyPressed(e);
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    // Send message via ENTER (on press)
                    sendMessage(tabPanel);
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                super.keyReleased(e);
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    // Send message via ENTER (on release)
                    sendMessage(tabPanel);
                }
            }
        });
        Util.findComponentIn(tabPanel, "sendButton").addMouseListener(new MouseInputAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                // Send message via send button
                sendMessage(tabPanel);
            }
        });

        // Selecting a user
        JList usersList = Util.findComponentIn(tabPanel, "usersList");
        usersList.addListSelectionListener(actionEvent -> {
            try {
                Util.<JTextArea>findComponentIn(tabPanel, "messageTextArea")
                        .setText("/w " + new NodeId(usersList.getSelectedValue().toString()).getUsername() + " ");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Adding a peer
        Util.<JButton>findComponentIn(tabPanel, "addPeerButton").addActionListener(actionEvent -> {
            String[] addPeerText =
                    Util.<JTextField>findComponentIn(tabPanel, "addPeerTextField").getText().split(":");
            if (addPeerText.length != 2) {
                chatTab.addNotification("When adding a peer please use the following format: IP_ADDRESS:PORT");
                return;
            }
            try {
                chatTab.getServer().getContext().addPeer(
                        new NodeId(Integer.parseInt(addPeerText[1]), addPeerText[0], "no-username")
                                .getNodeIdString()
                );
            } catch (NumberFormatException e) {
                chatTab.addNotification("When adding a peer please only use numbers in place of a port");
            }
        });
        return tabPanel;
    }

    private void sendMessage(JPanel tabPanel) {
        JTextArea textArea = Util.findComponentIn(tabPanel, "messageTextArea");

        // Trim newline
        String[] messages = textArea.getText().split("\n");
        textArea.setText("");

        ChatTab lookup = ChatTab.lookup(tabPanel);

        // Send via network
        for (String message : messages) {
            message = message.trim();
            if (!message.equals("")) {
                if (message.startsWith("/w ")) {
                    String[] split = message.substring(3, message.length()).split(" ");
                    if (split.length < 2) {
                        lookup.addNotification("Invalid whisper syntax, use the following format: /w name text");
                        continue;
                    }
                    lookup.getServer().addPrivateMessage(new PrivateMessage(
                            new Date(),
                            lookup.getServer().getNodeId().getUsername(),
                            split[0],
                            message.substring(message.lastIndexOf(split[1]), message.length())
                    ));
                } else {
                    lookup.getServer().announceMessage(
                            new Message(new Date(), lookup.getServer().getNodeId().getUsername(), message)
                    );
                }
            }
        }

        // Temporary
        //lookup.addMessage(lookup.getUsername(), messages, new Date());
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Decentralized Chat by Steve (ɔ) 2018->inf");
        frame.setContentPane(new ChatLayout().mainPanel);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
    }
}
