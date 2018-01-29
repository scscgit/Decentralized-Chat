package sk.tuke.ds.chat.rmi;

import sk.tuke.ds.chat.layouts.ChatTab;
import sk.tuke.ds.chat.messaging.Message;
import sk.tuke.ds.chat.messaging.PrivateMemory;
import sk.tuke.ds.chat.messaging.PrivateMessage;
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
import java.util.Optional;
import java.util.Set;

public class ChatNodeServer extends AbstractServer implements ChatNodeConnector {

    private final NodeId nodeId;
    private final HeartbeatImpl heartbeater;
    private NodeContext nodeContext;
    private PrivateMemory privateMemory = new PrivateMemory();
    private BlockchainProcess blockchainProcess;
    private ChatTab chatTab;
    private boolean successfullyStarted;

    /**
     * Creates RMI registry on specified port, exports the current server object and binds it to the registry.
     *
     * @param port        specified port
     * @param peerNodeIds the initial peers to start the server context with
     * @throws RemoteException
     */
    public ChatNodeServer(int port, List<String> peerNodeIds, boolean useUpnp) throws RemoteException, UnknownHostException {
        super(ChatNodeConnector.SERVICE_NAME, port, useUpnp);
        // Re-loading the port just in case future implementation changes
        port = getPort();
        try {
            this.nodeId = new NodeId(port, "no-username");
            // If there is a peer, then this instance should connect to existing chat instead of hosting a new Blockchain
            boolean success = false;
            if (peerNodeIds.size() > 0) {
                for (int i = 0; i < peerNodeIds.size(); i++) {
                    String peerNodeId = peerNodeIds.get(i);
                    HeartbeatConnector peer = Util.rmiTryLookup(new NodeId(peerNodeId), HeartbeatConnector.SERVICE_NAME);
                    if (peer == null) {
                        Log.e(this,
                                "Couldn't connect to peer " + peerNodeId
                                        + " (" + i + "/" + peerNodeIds.size()
                                        + ") to download the blockchain, trying others...");
                        continue;
                    }
                    this.nodeContext = new NodeContext(new HashSet<>(peerNodeIds), peer.getBlockchain());
                    success = true;
                    break;
                }
                if (!success) {
                    throw new RemoteException("Couldn't lookup any peer to download the initial blockchain");
                }
            } else {
                this.nodeContext = new NodeContext(new HashSet<>(peerNodeIds), new Blockchain());
            }
            Log.i(this, "Starting heartbeater (without UPnP)");
            this.heartbeater = new HeartbeatImpl(this, port, false);
            Log.i(this, "Starting blockchain process");
            this.blockchainProcess = new BlockchainProcess(this, this.nodeContext.getBlockchain());
            Log.i(this, "Node is now fully operational");
            this.successfullyStarted = true;
        } catch (Exception e) {
            // In the event of a constructor problem don't block RMI
            try {
                stop();
            } finally {
                throw e;
            }
        }
    }

    @Override
    public void stop() throws RemoteException {
        try {
            super.stop();
        } finally {
            try {
                if (this.heartbeater != null) {
                    this.heartbeater.stop();
                }
            } finally {
                try {
                    if (this.blockchainProcess != null) {
                        this.blockchainProcess.stop();
                    }
                } finally {
                    if (this.successfullyStarted) {
                        // Don't expand the log if there was an error
                        Log.i(this, "Node is now fully stopped");
                    }
                }
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
        Log.i(this,
                "[Server] <" + getNodeId().getUsername() + "> Received an unconfirmed message from "
                        + message.getUser() + ": " + message.getMessage());
        // Enqueue to be added to a next block
        this.blockchainProcess.addMessage(message);
    }

    @Override
    public void receiveAnnouncedBlock(Block block) {
        if (this.nodeContext.getBlockchain().addToBlockchain(block) != null) {
            Log.e(this,
                    "[Server] <" + getNodeId().getUsername() + "> Received incompatible block SHA "
                            + block.shaHash() + ", ignoring");
            // Assuming the heartbeat will take care of this, the other node should keep re-generating their messages
        } else {
            Log.i(this,
                    "[+ Server +] <" + getNodeId().getUsername() + "> Successfully received block number #"
                            + this.nodeContext.getBlockchain().lastBlockIndex() + ", SHA256 " + block.shaHash()
                            + " (containing " + block.getMessages().size() + " messages), processing");
            // Stop trying to mine the messages that are already processed
            block.getMessages().forEach(this.blockchainProcess::removeMinedMessage);
            // No need to keep mining the current block; it's guaranteed to be outdated, instead mine a new one ASAP
            // This is the part where concensus about choosing the true blockchain happens
            this.blockchainProcess.cancel();
            // The messages are now verified, so they should appear in the tab UI
            displayConfirmedMessages(block);
        }
    }

    @Override
    public void fastKickPeer(PrivateMessage privateMessage) throws RemoteException {
        // For now this is all handled in the method for adding of a new private message
        addPrivateMessage(privateMessage);
    }

    private void displayConfirmedMessages(Block block) {
        for (Message message : block.getMessages()) {
            this.chatTab.addMessage(message.getUser(), new String[]{message.getMessage()}, message.getDate());
        }
    }

    public void announceMessage(Message message) {
        // Announce to self too
        receiveAnnouncedMessage(message);
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
        // Don't announce to self; it is already added, only display them
        displayConfirmedMessages(block);
        int i = 0;
        for (String peerNodeIdString : this.nodeContext.getPeersCopy()) {
            ChatNodeConnector peer = Util.rmiTryLookup(new NodeId(peerNodeIdString), ChatNodeConnector.SERVICE_NAME);
            if (peer != null) {
                try {
                    peer.receiveAnnouncedBlock(block);
                    i++;
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
        Log.d(this, "Announced block SHA256 " + block.shaHash() + " to " + i + " peers");
    }

    public ChatTab getChatTab() {
        return chatTab;
    }

    public void setChatTab(ChatTab chatTab) {
        this.chatTab = chatTab;
        getContext().getBlockchain().displayBlocksInto(this::displayConfirmedMessages);
        // This activates the blockchain process
        this.chatTab.setInitialized();
    }

    public void setUsername(String username) {
        // This is now instead handled on the receiving side too, but without rename it'll get sent back
        this.privateMemory.rename(getNodeId().getUsername(), username);
        getNodeId().setUsername(username);
    }

    public PrivateMemory getPrivateMemory() {
        return privateMemory;
    }

    public void addPrivateMessage(PrivateMessage privateMessage) {
        boolean explicitlyToMe = privateMessage.getToUser().equals(getNodeId().getUsername());
        // Special testing HACK, message "#KICK" is not a private message!!!
        if (privateMessage.getMessage().equals("#KICK")) {
            if (explicitlyToMe) {
                // I am being kicked; kick everyone
                Log.i(this, "Got informed that I am being kicked, kicking all");
                getChatTab().addNotification("You were kicked by user " + privateMessage.getFromUser());
                getContext().getPeersCopy().forEach(getContext()::removePeer);
                getContext().getPeersUnconfirmedCopy().forEach(getContext()::removePeer);
                getChatTab().refreshPeers();
                return;
            }

            // Remove the peer with such username from peers list
            Optional<String> kickPeerNodeIdString = getContext()
                    .getPeersCopy()
                    .stream()
                    .filter(
                            peerNodeIdString -> new NodeId(peerNodeIdString).getUsername().equals(
                                    privateMessage.getToUser()
                            )
                    )
                    .findFirst();
            if (kickPeerNodeIdString.isPresent()) {
                getContext().removePeer(kickPeerNodeIdString.get());
                getChatTab().refreshPeers();
                this.chatTab.addNotification("Kicked peer " + kickPeerNodeIdString.get());
                Set<String> peersToTellAboutKick = getContext().getPeersCopy();
                // Don't forget to include the kicked out one in the list of messages of peers being told about kick!
                peersToTellAboutKick.add(kickPeerNodeIdString.get());

                // Disseminate the kick
                peersToTellAboutKick.forEach(
                        peerNodeIdString -> {
                            Optional.ofNullable(Util.<ChatNodeConnector>rmiTryLookup(
                                    new NodeId(peerNodeIdString), ChatNodeConnector.SERVICE_NAME
                            )).ifPresent(peer -> {
                                try {
                                    peer.fastKickPeer(privateMessage);
                                    Log.d(this, "Told " + peerNodeIdString + " about the kick");
                                } catch (RemoteException e) {
                                    e.printStackTrace();
                                }
                            });
                        }
                );
            } else {
                Log.e(this,
                        "Couldn't kick the peer " + privateMessage.getToUser()
                                + " as no such user was listed");
            }
            return;
        }
        // Store and display the private message
        getPrivateMemory().add(privateMessage);
        displayPrivateMessage(privateMessage);
    }

    public void displayPrivateMessage(PrivateMessage privateMessage) {
        boolean received = !privateMessage.getFromUser().equals(getNodeId().getUsername());
        getChatTab().addPrivateMessage(
                received ? privateMessage.getFromUser() : privateMessage.getToUser(),
                privateMessage.getMessage(),
                privateMessage.getDate(),
                received
        );
    }
}
