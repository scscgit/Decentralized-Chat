package sk.tuke.ds.chat.rmi.abstraction;

import sk.tuke.ds.chat.messaging.Message;
import sk.tuke.ds.chat.node.Block;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ChatNodeConnector extends Remote {

    String SERVICE_NAME = "ChatNodeService";

    void receiveAnnouncedMessage(Message message) throws RemoteException;

    void receiveAnnouncedBlock(Block block) throws RemoteException;
}
