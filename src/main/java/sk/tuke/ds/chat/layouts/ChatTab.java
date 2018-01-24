package sk.tuke.ds.chat.layouts;

import sk.tuke.ds.chat.rmi.ChatNodeServer;
import sk.tuke.ds.chat.util.Util;

import javax.swing.*;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class ChatTab {

    private static final Map<JPanel, ChatTab> chatTabMap = new HashMap<>();

    private JPanel tabPanel;
    private String username;
    private ChatNodeServer server;

    private ChatTab(JPanel tabPanel) {
        if (tabPanel == null) {
            throw new RuntimeException("Null value of tab panel provided");
        }
        this.tabPanel = tabPanel;
    }

    public static synchronized ChatTab lookup(JPanel tabPanel) {
        if (!ChatTab.chatTabMap.containsKey(tabPanel)) {
            ChatTab.chatTabMap.put(tabPanel, new ChatTab(tabPanel));
        }
        return ChatTab.chatTabMap.get(tabPanel);
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
        Util.<JTextField>findComponentIn(tabPanel, "usernameTextField").setText(username);
    }

    public void generateUsername() {
        setUsername("guest" + (new Random().nextInt(900) + 100));
    }

    public ChatNodeServer getServer() {
        return server;
    }

    public void setServer(ChatNodeServer server) {
        this.server = server;
    }

    public void onDisconnect() {
        try {
            this.server.stop();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void addMessage(String username, String[] messages, Date date) {
        JTextPane messagesTextPane = Util.findComponentIn(this.tabPanel, "messagesTextPane");
        for (String message : messages) {
            message = message.trim();
            if (!"".equals(message)) {
                messagesTextPane.setText(
                        messagesTextPane.getText()
                                + "\n"
                                + (
                                new SimpleDateFormat("dd.M. hh:mm:ss").format(date)
                                        + " ["
                                        + username
                                        + "] > "
                                        + message)
                );
            }
        }
    }
}
