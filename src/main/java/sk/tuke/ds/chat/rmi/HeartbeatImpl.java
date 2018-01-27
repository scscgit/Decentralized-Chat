package sk.tuke.ds.chat.rmi;

import sk.tuke.ds.chat.node.Blockchain;
import sk.tuke.ds.chat.node.NodeContext;
import sk.tuke.ds.chat.node.NodeId;
import sk.tuke.ds.chat.rmi.abstraction.AbstractProcess;
import sk.tuke.ds.chat.rmi.abstraction.AbstractServer;
import sk.tuke.ds.chat.rmi.abstraction.HeartbeatConnector;
import sk.tuke.ds.chat.util.Log;
import sk.tuke.ds.chat.util.Util;

import java.rmi.RemoteException;
import java.util.Set;

public class HeartbeatImpl extends AbstractServer implements HeartbeatConnector {

    private final ChatNodeServer chatNodeServer;
    private final AbstractProcess heartbeatProcess;

    /**
     * Creates RMI registry on specified port, exports the current server object and binds it to the registry.
     *
     * @param chatNodeServer the node that is doing the heartbeating
     * @param port           specified port
     * @throws RemoteException
     */
    public HeartbeatImpl(ChatNodeServer chatNodeServer, int port) throws RemoteException {
        super(HeartbeatConnector.SERVICE_NAME, port);
        this.chatNodeServer = chatNodeServer;

        // Running the initial heartbeat
        HeartbeatImpl.this.chatNodeServer.getContext().getPeersCopy().forEach(
                peerNodeId -> {
                    if (!sendHeartbeat(new NodeId(peerNodeId))) {
                        // This mustn't fail
                        throw new RuntimeException("Couldn't send initial heartbeat");
                    }
                }
        );

        this.heartbeatProcess = new AbstractProcess() {
            @Override
            public void run() {
                while (isRunning()) {
                    Log.i(this, "Running heartbeat from " + chatNodeServer.getNodeId().getNodeIdString());
                    HeartbeatImpl.this.chatNodeServer.getContext().getPeersCopy().forEach(
                            peerNodeId -> {
                                sendHeartbeat(new NodeId(peerNodeId));
                            }
                    );
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        // Starting the parallel process
        this.heartbeatProcess.start();
    }

    private boolean sendHeartbeat(NodeId toNodeId) {
        NodeContext receivedContext;
        try {
            HeartbeatConnector peer = Util.rmiTryLookup(toNodeId, HeartbeatConnector.SERVICE_NAME);
            if (peer == null) {
                throw new RemoteException("Couldn't lookup peer via RMI");
            }
            try {
                receivedContext = peer.receiveHeartbeat(
                        // Sender description
                        this.chatNodeServer.getNodeId().getNodeIdString(),
                        // Confirmation of receiver node ID, so that there are no duplicate entries
                        toNodeId.getNodeIdString(),
                        // Copying NodeContext to try to prevent ConcurrentModificationException
                        new NodeContext(
                                this.chatNodeServer.getContext().getPeersCopy(),
                                this.chatNodeServer.getContext().getBlockchain()
                        )
                );
            } catch (NodeIdOutdatedException nodeIdOutdated) {
                this.chatNodeServer.getContext().removePeer(toNodeId.getNodeIdString());
                this.chatNodeServer.getContext().addPeer(nodeIdOutdated.getCorrectNodeId());
                Log.e(this,
                        "Replaced invalid node ID " + toNodeId.getNodeIdString()
                                + " by " + nodeIdOutdated.getCorrectNodeId() + " on demand");
                refreshPeers();
                // This exception is not an error
                receivedContext = nodeIdOutdated.getNodeContext();
            }
            // Processing the received context, specifying ignored peers and then adding the rest
            Set<String> knownPeersOrSelf = this.chatNodeServer.getContext().getPeersCopy();
            // Skip self too
            knownPeersOrSelf.add(this.chatNodeServer.getNodeId().getNodeIdString());
            receivedContext.getPeersCopy()
                    .stream()
                    // Filter only ones whose IP and Host Address combination aren't added yet
                    .filter(receivedPeer -> {
                                String receivedPeerAddressOnly =
                                        new NodeId(receivedPeer).getPort()
                                                + ":"
                                                + new NodeId(receivedPeer).getHostAddress();
                                return knownPeersOrSelf
                                        .stream()
                                        .map(peerNodeIdString ->
                                                new NodeId(peerNodeIdString).getPort()
                                                        + ":"
                                                        + new NodeId(peerNodeIdString).getHostAddress()
                                        ).noneMatch(peerAddressOnly -> peerAddressOnly.equals(receivedPeerAddressOnly));
                            }
                    )
                    .forEach(newReceivedPeer -> {
                        // Try the validity of the peer (don't add dead nodes)
                        try {
                            Util.rmiTryLookup(new NodeId(newReceivedPeer), HeartbeatConnector.SERVICE_NAME);
                        } catch (Exception e) {
                            Log.e(this, "[Heartbeat] Not adding a dead peer " + newReceivedPeer);
                            return;
                        }
                        Log.i(this,
                                "[Heartbeat] Learned new peer " + newReceivedPeer + " from a peer " + toNodeId);
                        this.chatNodeServer.getContext().addPeer(newReceivedPeer);
                        refreshPeers();
                    });
            return true;
        } catch (RemoteException e) {
            Log.e(this,
                    "[Heartbeat] Send failed to " + toNodeId.getNodeIdString() + ", removing from list");
            HeartbeatImpl.this.chatNodeServer.getContext().removePeer(toNodeId.getNodeIdString());
            refreshPeers();
            e.printStackTrace();
            return false;
        }
    }

    private void refreshPeers() {
        // Is null during the first heartbeat
        if (this.chatNodeServer.getChatTab() != null) {
            this.chatNodeServer.getChatTab().refreshPeers();
        }
    }

    @Override
    public NodeContext receiveHeartbeat(String fromNodeId, String toThisNodeId, NodeContext nodeContext) throws RemoteException, NodeIdOutdatedException {
        String thisNodeIdString = this.chatNodeServer.getNodeId().getNodeIdString();
        if (this.chatNodeServer.getContext().addPeer(fromNodeId)) {
            Log.i(this,
                    "[+ Heartbeat +] Received from a new peer " + fromNodeId
                            + " on the node " + thisNodeIdString
            );
            refreshPeers();
        }
        if (this.chatNodeServer.getContext().getBlockchain().joinBlockchain(
                nodeContext.getBlockchain(),
                this.chatNodeServer
        )) {
            Log.e(this,
                    "Blockchain of peer " + fromNodeId
                            + " JOINED on the receiver node " + thisNodeIdString);
        } else {
            Log.d(this, "Blockchains were OK");
        }
        // Successful termination; either via node id correction, or a normal return
        if (!thisNodeIdString.equals(toThisNodeId)) {
            Log.e(this,
                    "[! Heartbeat !] Received to a wrong node id, " + toThisNodeId
                            + " instead of " + thisNodeIdString + " - correcting the sender");
            throw new NodeIdOutdatedException(thisNodeIdString, this.chatNodeServer.getContext());
        }
        return this.chatNodeServer.getContext();
    }

    @Override
    public Blockchain getBlockchain() throws RemoteException {
        return this.chatNodeServer.getContext().getBlockchain();
    }

    @Override
    public void stop() throws RemoteException {
        super.stop();
        this.heartbeatProcess.stop();
    }
}
