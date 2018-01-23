package sk.tuke.ds.chat.layouts;

import javafx.scene.input.KeyCode;
import sk.tuke.ds.chat.util.Util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

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
        messageTextArea.setName("messageTextArea");
        messagesTextPane.setName("messagesTextPane");

        connectButton.addActionListener(e -> {
            // Connect to IP
            tabbedPane.addTab(
                    nodeIpTextField.getText() + ":" + nodePortTextField.getText(),
                    generateListeners(Util.copySerializable(templateTab))
            );
        });
        hostButton.addActionListener(e -> {
            // Host new server
        });

        // Example template tab is also used to test the listener generation on (Optional for testing)
        generateListeners(templateTab);

        // Hide the template though
        this.tabbedPane.remove(templateTab);
    }

    private <T extends JPanel> T generateListeners(T tabPanel) {
        Util.findComponentIn(tabPanel, "messageTextArea").addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                super.keyReleased(e);
                JTextArea tabMessageTextArea = Util.findComponentIn(tabPanel, "messageTextArea");
                String trimmedMessage = tabMessageTextArea.getText().trim();
                if (trimmedMessage.equals("\n") || trimmedMessage.equals("")) {
                    return;
                }
                if (e.getKeyCode() == KeyCode.ENTER.impl_getCode()) {
                    // Send message via ENTER
                    sendMessage(tabMessageTextArea);
                }
            }
        });
        return tabPanel;
    }

    private void sendMessage(JTextArea textArea) {
        // Trim newline
        String message = textArea.getText().substring(0, textArea.getText().length() - 2);
        textArea.setText("");

        // TODO send via network

        boolean found = false;
        for (Component component : textArea.getParent().getComponents()) {
            if ("messagesTextPane".equals(component.getName())) {
                JTextPane messages = (JTextPane) component;
                messages.setText(messages.getText() + "\n" + "MSG > " + message);
                found = true;
                break;
            }
        }
        if (!found) {
            throw new RuntimeException("messagesTextPane not found in parent" + textArea.getParent().getName());
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("sk.tuke.ds.chat.ChatLayout");
        frame.setContentPane(new ChatLayout().mainPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
    }
}
