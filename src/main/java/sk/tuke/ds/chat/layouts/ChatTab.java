package sk.tuke.ds.chat.layouts;

import sk.tuke.ds.chat.rmi.ChatNodeServer;
import sk.tuke.ds.chat.util.Util;

import javax.swing.*;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ChatTab {

    private static final Map<JPanel, ChatTab> chatTabMap = new HashMap<>();

    private JPanel tabPanel;
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

    public void setUsername(String username) {
        Util.<JTextField>findComponentIn(tabPanel, "usernameTextField").setText(username);
        // Update other nodes' knowledge of this change on heartbeat
        getServer().getNodeId().setUsername(username);
    }

    public void generateUsername() {
        setUsername("guest" + (new Random().nextInt(900) + 100));
    }

    public ChatNodeServer getServer() {
        return server;
    }

    public void setServer(ChatNodeServer server) {
        this.server = server;
        this.server.setChatTab(this);
        refreshPeers();
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

    public void refreshPeers() {
        Util.<JList>findComponentIn(tabPanel, "usersList").removeAll();
        List<String> peers = new ArrayList<>(getServer().getContext().getPeersCopy());
        Util.<JList>findComponentIn(tabPanel, "usersList").setModel(
                new AbstractListModel<String>() {
                    public int getSize() {
                        return peers.size();
                    }

                    public String getElementAt(int i) {
                        return peers.get(i);
                    }
                }
        );
        Util.savePeersConfiguration(peers, getServer().getNodeId().getPort());
    }
}
