package sk.tuke.ds.chat.layouts;

import sk.tuke.ds.chat.util.Util;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ChatLayout {
    private JTabbedPane tabbedPane1;
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
    private JTextArea textArea1;
    private JButton sendButton;

    public ChatLayout() {
        connectButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Connect to IP
                tabbedPane1.addTab(
                        nodeIpTextField.getText() + ":" + nodePortTextField.getText(),
                        Util.copySerializable(tabbedPane1.getComponentAt(1))
                );
            }
        });
        hostButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Host new server
            }
        });
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
