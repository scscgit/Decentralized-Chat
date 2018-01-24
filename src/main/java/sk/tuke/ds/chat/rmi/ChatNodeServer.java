package sk.tuke.ds.chat.rmi;

import sk.tuke.ds.chat.node.NodeContext;
import sk.tuke.ds.chat.node.NodeId;
import sk.tuke.ds.chat.rmi.abstraction.AbstractServer;
import sk.tuke.ds.chat.rmi.abstraction.ChatNodeConnector;

import java.net.UnknownHostException;
import java.rmi.RemoteException;

public class ChatNodeServer extends AbstractServer implements ChatNodeConnector {

    private final NodeId nodeId;
    private final HeartbeatImpl heartbeater;
    private NodeContext nodeContext;

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
}
