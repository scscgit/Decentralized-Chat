package sk.tuke.ds.chat.rmi;

import sk.tuke.ds.chat.layouts.ChatTab;
import sk.tuke.ds.chat.node.*;
import sk.tuke.ds.chat.rmi.abstraction.AbstractServer;
import sk.tuke.ds.chat.rmi.abstraction.ChatNodeConnector;
import sk.tuke.ds.chat.util.Log;
import sk.tuke.ds.chat.util.Util;

import java.net.UnknownHostException;
import java.rmi.RemoteException;

public class ChatNodeServer extends AbstractServer implements ChatNodeConnector {

    private final NodeId nodeId;
    private final HeartbeatImpl heartbeater;
    private NodeContext nodeContext;
    private Blockchain blockchain;
    private BlockchainProcess blockchainProcess;
    private ChatTab chatTab;

    /**
     * Creates RMI registry on specified port, exports the current server object and binds it to the registry.
     *
     * @param port        specified port
     * @param nodeContext the initial context to start the server with, specifically existing peers to try
     * @throws RemoteException
     */
    public ChatNodeServer(int port, NodeContext nodeContext) throws RemoteException, UnknownHostException {
        super(ChatNodeConnector.SERVICE_NAME, port);
        this.nodeId = new NodeId(port);
        this.heartbeater = new HeartbeatImpl(this, port);
        this.nodeContext = nodeContext;
        this.blockchain = new Blockchain();
        this.blockchainProcess = new BlockchainProcess(this, this.blockchain);
    }

    @Override
    public void stop() throws RemoteException {
        try {
            super.stop();
        } finally {
            this.heartbeater.stop();
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
        if (!this.blockchain.addToBlockchain(block)) {
            Log.e(this, "Received incompatible block SHA " + block.shaHash() + ", ignoring");
            // Assuming the heartbeat will take care of this
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
        receiveAnnouncedMessage(message);
        int i = 0;
        for (String peerNodeIdString : this.nodeContext.getPeers()) {
            ChatNodeConnector peer = Util.rmiLookup(new NodeId(peerNodeIdString), ChatNodeConnector.SERVICE_NAME);
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
        receiveAnnouncedBlock(block);
        int i = 0;
        for (String peerNodeIdString : this.nodeContext.getPeers()) {
            ChatNodeConnector peer = Util.rmiLookup(new NodeId(peerNodeIdString), ChatNodeConnector.SERVICE_NAME);
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

    public void setChatTab(ChatTab chatTab) {
        this.chatTab = chatTab;
    }
}
