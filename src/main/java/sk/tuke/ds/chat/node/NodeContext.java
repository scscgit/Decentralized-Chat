package sk.tuke.ds.chat.node;

import java.util.ArrayList;
import java.util.List;

public class NodeContext {

    private List<String> peers;
    private List<Message> messages;

    public NodeContext() {
        this.peers = new ArrayList<>();
        this.messages = new ArrayList<>();
    }

    public NodeContext(List<String> peers, List<Message> messages) {
        this.peers = peers;
        this.messages = messages;
    }

    public List<String> getPeers() {
        return peers;
    }

    public List<Message> getMessages() {
        return messages;
    }
}
