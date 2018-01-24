package sk.tuke.ds.chat.rmi;

import sk.tuke.ds.chat.node.NodeContext;
import sk.tuke.ds.chat.node.NodeId;
import sk.tuke.ds.chat.rmi.abstraction.AbstractProcess;
import sk.tuke.ds.chat.rmi.abstraction.AbstractServer;
import sk.tuke.ds.chat.rmi.abstraction.HeartbeatConnector;
import sk.tuke.ds.chat.util.Log;

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
        this.heartbeatProcess = new AbstractProcess() {
            @Override
            public void run() {
                while (isRunning()) {
                    Log.i(this, "Running heartbeat from " + chatNodeServer.getNodeId().getNodeIdString());
                    HeartbeatImpl.this.chatNodeServer.getContext().getPeers().forEach(
                            peerNodeId -> sendHeartbeat(new NodeId(peerNodeId))
                    );
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        this.heartbeatProcess.start();
    }

    private void sendHeartbeat(NodeId toNodeId) {
        // TODO
    }

    @Override
    public void receiveHeartbeat(String fromNodeId, NodeContext nodeContext) throws RemoteException {
        // TODO
    }

    @Override
    public void stop() throws RemoteException {
        super.stop();
        this.heartbeatProcess.stop();
    }
}
