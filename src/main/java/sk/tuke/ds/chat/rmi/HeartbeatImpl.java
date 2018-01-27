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
                    try {
                        sendHeartbeat(new NodeId(peerNodeId));
                    } catch (RemoteException e) {
                        e.printStackTrace();
                        // This mustn't fail
                        throw new RuntimeException(e);
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
                                try {
                                    sendHeartbeat(new NodeId(peerNodeId));
                                } catch (RemoteException e) {
                                    Log.e(this, "[Heartbeat] Send failed to " + peerNodeId);
                                    e.printStackTrace();
                                    HeartbeatImpl.this.chatNodeServer.getContext().removePeer(peerNodeId);
                                }
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

    private void sendHeartbeat(NodeId toNodeId) throws RemoteException {
        HeartbeatConnector peer = Util.rmiTryLookup(toNodeId, HeartbeatConnector.SERVICE_NAME);
        if (peer == null) {
            throw new RemoteException("Couldn't lookup a peer");
        }
        try {
            NodeContext receivedContext = peer.receiveHeartbeat(
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
            receivedContext.getPeersCopy()
                    .stream()
                    .filter(receivedPeer -> !this.chatNodeServer.getContext().getPeersCopy().contains(receivedPeer))
                    .filter(receivedPeer -> !receivedPeer.equals(this.chatNodeServer.getNodeId().getNodeIdString()))
                    .forEach(newReceivedPeer -> {
                        Log.i(this,
                                "[Heartbeat] Learned new peer " + newReceivedPeer + " from a peer " + toNodeId);
                        this.chatNodeServer.getContext().addPeer(newReceivedPeer);
                    });
        } catch (NodeIdOutdatedException nodeIdOutdated) {
            this.chatNodeServer.getContext().removePeer(toNodeId.getNodeIdString());
            this.chatNodeServer.getContext().addPeer(nodeIdOutdated.getCorrectNodeId());
            Log.e(this,
                    "Replaced invalid node ID " + toNodeId.getNodeIdString()
                            + " by " + nodeIdOutdated.getCorrectNodeId() + " on demand");
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
        }
        if (this.chatNodeServer.getContext().getBlockchain().joinBlockchain(nodeContext.getBlockchain())) {
            Log.e(this,
                    "Blockchain of peer " + fromNodeId
                            + " JOINED on the receiver node " + thisNodeIdString);
        } else {
            Log.d(this, "Blockchains were OK");
        }
        if (!thisNodeIdString.equals(toThisNodeId)) {
            Log.e(this,
                    "[! Heartbeat !] Received to a wrong node id, " + toThisNodeId
                            + " instead of " + thisNodeIdString + " - correcting the sender");
            throw new NodeIdOutdatedException(thisNodeIdString);
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
