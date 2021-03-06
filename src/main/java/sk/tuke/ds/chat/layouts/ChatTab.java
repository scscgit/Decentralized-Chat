package sk.tuke.ds.chat.layouts;

import sk.tuke.ds.chat.rmi.ChatNodeServer;
import sk.tuke.ds.chat.util.Log;
import sk.tuke.ds.chat.util.Util;

import javax.swing.*;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ChatTab {

    private static final Map<JPanel, ChatTab> chatTabMap = new HashMap<>();

    private JPanel tabPanel;
    private ChatNodeServer server;
    private boolean initialized;

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
        getServer().setUsername(username);
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
            Log.d(this,
                    "[Server] <" + getServer().getNodeId().getUsername() + "> " +
                            "displayed a confirmed message from " + username + ": " + message);
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

    public void addPrivateMessage(String username, String message, Date date, boolean received) {
        JTextPane messagesTextPane = Util.findComponentIn(this.tabPanel, "messagesTextPane");
        Log.d(this,
                "[Server] <" + getServer().getNodeId().getUsername() + "> " +
                        "received PM from " + username + ": " + message);
        messagesTextPane.setText(
                messagesTextPane.getText()
                        + "\n"
                        + (
                        new SimpleDateFormat("dd.M. hh:mm:ss").format(date)
                                + (received ? (" $ Private Message $ FROM [") : (" $ Private Message $ TO ["))
                                + username
                                + (received ? ("] < ") : ("] > "))
                                + message)
        );
    }

    public void refreshPeers() {
        // Note: this delimiter has to be able to be split on ":" and indexed as [2]. It's decorative only though
        final String unconfirmedDelimiter = " :(Unconfirmed): ";
        Util.<JList>findComponentIn(tabPanel, "usersList").removeAll();
        List<String> peers = new ArrayList<>(getServer().getContext().getPeersCopy());
        Set<String> peersUnconfirmedCopy = getServer().getContext().getPeersUnconfirmedCopy();
        if (peersUnconfirmedCopy.size() > 0) {
            peers.add(unconfirmedDelimiter);
        }
        peers.addAll(peersUnconfirmedCopy);
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
        List<String> realPeersWithoutDelimiter = new ArrayList<>(peers);
        realPeersWithoutDelimiter.remove(unconfirmedDelimiter);
        Util.savePeersConfiguration(peers, getServer().getNodeId().getPort());
    }

    public boolean isInitialized() {
        return this.initialized;
    }

    public void setInitialized() {
        this.initialized = true;
    }

    public void addNotification(String notification) {
        JTextPane messagesTextPane = Util.findComponentIn(this.tabPanel, "messagesTextPane");
        messagesTextPane.setText(
                messagesTextPane.getText()
                        + "\n"
                        + " * Notification: "
                        + notification
        );
    }

    public void clearMessages() {
        JTextPane messagesTextPane = Util.findComponentIn(this.tabPanel, "messagesTextPane");
        messagesTextPane.setText(
                "* Blockchain incompatible, fully reset and joined other peers"
        );
    }
}
