package sk.tuke.ds.chat.rmi.abstraction;

import sk.tuke.ds.chat.node.NodeContext;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface HeartbeatConnector extends Remote {

    String SERVICE_NAME = "HeartbeatService";

    void receiveHeartbeat(String nodeId, NodeContext nodeContext) throws RemoteException;
}
