package sk.tuke.ds.chat.messaging;

import sk.tuke.ds.chat.rmi.ChatNodeServer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PrivateMemory implements Serializable {

    public List<PrivateMessage> privateMessages = new ArrayList<>();

    public synchronized List<PrivateMessage> loadByUsername(String username) {
        return privateMessages
                .stream()
                .filter(message -> message.getFromUser().equals(username) || message.getToUser().equals(username))
                .collect(Collectors.toList());
    }

    public synchronized void add(PrivateMessage message) {
        this.privateMessages.add(message);
    }

    public synchronized void rename(String oldUsername, String newUsername) {
        privateMessages
                .stream()
                .filter(message -> message.getFromUser().equals(oldUsername))
                .forEach(message -> message.setFromUser(newUsername));
        privateMessages
                .stream()
                .filter(message -> message.getToUser().equals(oldUsername))
                .forEach(message -> message.setToUser(newUsername));
    }

    public synchronized void displayAll(ChatNodeServer chatNodeServer) {
        this.privateMessages.forEach(chatNodeServer::displayPrivateMessage);
    }
}
