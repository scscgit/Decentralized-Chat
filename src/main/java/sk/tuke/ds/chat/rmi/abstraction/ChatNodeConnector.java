package sk.tuke.ds.chat.rmi.abstraction;

import java.rmi.Remote;

public interface ChatNodeConnector extends Remote {

    String SERVICE_NAME = "ChatNodeService";
}
