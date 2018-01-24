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
     * @param port specified port
     * @throws RemoteException
     */
    public ChatNodeServer(int port) throws RemoteException, UnknownHostException {
        super(ChatNodeConnector.SERVICE_NAME, port);
        this.nodeId = new NodeId(port);
        this.heartbeater = new HeartbeatImpl(this, port);
        this.nodeContext = new NodeContext();
    }

    @Override
    public void stop() throws RemoteException {
        super.stop();
        this.heartbeater.stop();
    }

    public NodeId getNodeId() {
        return this.nodeId;
    }

    public NodeContext getContext() {
        return this.nodeContext;
    }
}
