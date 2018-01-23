package sk.tuke.ds.chat.layouts;

import javafx.scene.input.KeyCode;
import sk.tuke.ds.chat.util.Util;

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Date;

public class ChatLayout {
    private JTabbedPane tabbedPane;
    private JPanel mainPanel;
    private JTextField nodeIpTextField;
    private JTextField nodePortTextField;
    private JButton connectButton;
    private JButton hostButton;
    private JTextField hostPortTextField;
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

    public ChatLayout() {
        // Manually set names for the objects that are gonna be looked up (including their cloned versions)
        this.messageTextArea.setName("messageTextArea");
        this.messagesTextPane.setName("messagesTextPane");
        this.usernameTextField.setName("usernameTextField");

        // Configuration listeners
        this.connectButton.addActionListener(e -> {
            // Connect to IP
            JPanel tabPanel = generateListeners(Util.copySerializable(this.templateTab));
            this.tabbedPane.addTab(
                    this.nodeIpTextField.getText() + ":" + this.nodePortTextField.getText(),
                    tabPanel
            );
            this.tabbedPane.setSelectedComponent(tabPanel);
            ChatTab.lookup(tabPanel).generateUsername();
        });
        this.hostButton.addActionListener(e -> {
            // Host new server
        });

        // Example template tab is also used to test the listener generation on (Optional for testing)
        generateListeners(this.templateTab);

        // Hide the template though
        this.tabbedPane.remove(this.templateTab);
    }

    private <T extends JPanel> T generateListeners(T tabPanel) {
        // Tab configuration
        this.renameUserButton.addActionListener(e -> {
            ChatTab.lookup(tabPanel).setUsername(
                    Util.<JTextField>findComponentIn(tabPanel, "usernameTextField").getText()
            );
        });
        this.disconnectButton.addActionListener(e -> {
            this.tabbedPane.remove(tabPanel);
            ChatTab.lookup(tabPanel).onDisconnect();
        });

        // Sending messages
        Util.findComponentIn(tabPanel, "messageTextArea").addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                super.keyReleased(e);
                if (e.getKeyCode() == KeyCode.ENTER.impl_getCode()) {
                    // Send message via ENTER
                    sendMessage(tabPanel);
                }
            }
        });
        return tabPanel;
    }

    private void sendMessage(JPanel tabPanel) {
        JTextArea textArea = Util.findComponentIn(tabPanel, "messageTextArea");

        // Trim newline
        String[] messages = textArea.getText().split("\n");
        textArea.setText("");

        // TODO send via network

        // Temporary
        ChatTab lookup = ChatTab.lookup(tabPanel);
        lookup.addMessage(lookup.getUsername(), messages, new Date());
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Decentralized Chat by Steve (ɔ)");
        frame.setContentPane(new ChatLayout().mainPanel);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
    }
}
