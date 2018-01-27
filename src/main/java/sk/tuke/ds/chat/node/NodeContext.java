package sk.tuke.ds.chat.node;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class NodeContext implements Serializable {

    private Set<String> peers;
    private Blockchain blockchain;

    public NodeContext(Set<String> peers, Blockchain blockchain) {
        this.peers = peers;
        this.blockchain = blockchain;
    }

    public synchronized Set<String> getPeersCopy() {
        return new HashSet<>(peers);
    }

    public Blockchain getBlockchain() {
        return blockchain;
    }

    public synchronized boolean addPeer(String peerNodeIdString) {
        return !this.peers.contains(peerNodeIdString) && this.peers.add(peerNodeIdString);
    }

    public synchronized void removePeer(String peerNodeIdString) {
        this.peers.remove(peerNodeIdString);
    }
}
