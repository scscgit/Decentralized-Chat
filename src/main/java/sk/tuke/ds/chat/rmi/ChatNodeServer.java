package sk.tuke.ds.chat.rmi;

import sk.tuke.ds.chat.layouts.ChatTab;
import sk.tuke.ds.chat.node.*;
import sk.tuke.ds.chat.rmi.abstraction.AbstractServer;
import sk.tuke.ds.chat.rmi.abstraction.ChatNodeConnector;
import sk.tuke.ds.chat.rmi.abstraction.HeartbeatConnector;
import sk.tuke.ds.chat.util.Log;
import sk.tuke.ds.chat.util.Util;

import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.List;

public class ChatNodeServer extends AbstractServer implements ChatNodeConnector {

    private final NodeId nodeId;
    private final HeartbeatImpl heartbeater;
    private NodeContext nodeContext;
    private BlockchainProcess blockchainProcess;
    private ChatTab chatTab;

    /**
     * Creates RMI registry on specified port, exports the current server object and binds it to the registry.
     *
     * @param port        specified port
     * @param peerNodeIds the initial peers to start the server context with
     * @throws RemoteException
     */
    public ChatNodeServer(int port, List<String> peerNodeIds) throws RemoteException, UnknownHostException {
        super(ChatNodeConnector.SERVICE_NAME, port);
        try {
            this.nodeId = new NodeId(port, "no-username");
            // If there is a peer, then this instance should connect to existing chat instead of hosting a new Blockchain
            if (peerNodeIds.size() > 0) {
                HeartbeatConnector peer = Util.rmiTryLookup(new NodeId(peerNodeIds.get(0)), HeartbeatConnector.SERVICE_NAME);
                if (peer == null) {
                    throw new RemoteException("Couldn't lookup a peer");
                }
                this.nodeContext = new NodeContext(new HashSet<>(peerNodeIds), peer.getBlockchain());
            } else {
                this.nodeContext = new NodeContext(new HashSet<>(peerNodeIds), new Blockchain());
            }
            this.heartbeater = new HeartbeatImpl(this, port);
            this.blockchainProcess = new BlockchainProcess(this, this.nodeContext.getBlockchain());
        } catch (Exception e) {
            // In the event of a constructor problem don't block RMI
            stop();
            throw e;
        }
    }

    @Override
    public void stop() throws RemoteException {
        try {
            super.stop();
        } finally {
            try {
                this.heartbeater.stop();
            } finally {
                this.blockchainProcess.stop();
            }
        }
    }

    public NodeId getNodeId() {
        return this.nodeId;
    }

    public NodeContext getContext() {
        return this.nodeContext;
    }

    @Override
    public void receiveAnnouncedMessage(Message message) {
        // Enqueue to be added to a next block
        this.blockchainProcess.addMessage(message);
    }

    @Override
    public void receiveAnnouncedBlock(Block block) {
        if (this.nodeContext.getBlockchain().addToBlockchain(block) != null) {
            Log.e(this, "Received incompatible block SHA " + block.shaHash() + ", ignoring");
            // Assuming the heartbeat will take care of this, the other node should keep re-generating their messages
        } else {
            // Stop trying to mine the messages that are already processed
            block.getMessages().forEach(this.blockchainProcess::removeMinedMessage);
            // No need to keep mining the current block; it's guaranteed to be outdated, instead mine a new one ASAP
            this.blockchainProcess.cancel();
            // The messages are now verified, so they should appear in the tab UI
            for (Message message : block.getMessages()) {
                this.chatTab.addMessage(message.getUser(), new String[]{message.getMessage()}, message.getDate());
            }
        }
    }

    public void announceMessage(Message message) {
        // Don't announce to self; it is already added
        int i = 0;
        for (String peerNodeIdString : this.nodeContext.getPeersCopy()) {
            ChatNodeConnector peer = Util.rmiTryLookup(new NodeId(peerNodeIdString), ChatNodeConnector.SERVICE_NAME);
            if (peer != null) {
                try {
                    peer.receiveAnnouncedMessage(message);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                i++;
            }
        }
        Log.d(this, "Announced message from " + message.getUser() + " to " + i + " peers");
    }

    public void announceBlock(Block block) {
        // Don't announce to self; it is already added
        int i = 0;
        for (String peerNodeIdString : this.nodeContext.getPeersCopy()) {
            ChatNodeConnector peer = Util.rmiTryLookup(new NodeId(peerNodeIdString), ChatNodeConnector.SERVICE_NAME);
            if (peer != null) {
                try {
                    peer.receiveAnnouncedBlock(block);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                i++;
            }
        }
        Log.d(this, "Announced block SHA256 " + block.shaHash() + " to " + i + " peers");
    }

    public ChatTab getChatTab() {
        return chatTab;
    }

    public void setChatTab(ChatTab chatTab) {
        this.chatTab = chatTab;
    }
}
