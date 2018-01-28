package sk.tuke.ds.chat.rmi.abstraction;

import sk.tuke.ds.chat.messaging.PrivateMessage;
import sk.tuke.ds.chat.node.Blockchain;
import sk.tuke.ds.chat.node.NodeContext;
import sk.tuke.ds.chat.rmi.NodeIdOutdatedException;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface HeartbeatConnector extends Remote {

    String SERVICE_NAME = "HeartbeatService";

    NodeContext receiveHeartbeat(
            String fromNodeId,
            String toThisNodeId,
            NodeContext nodeContext,
            List<PrivateMessage> privateMessages
    ) throws RemoteException, NodeIdOutdatedException;

    Blockchain getBlockchain() throws RemoteException;

    //List<PrivateMessage> getPrivateCommunication(String username) throws RemoteException;
}
