package sk.tuke.ds.chat.node;

import java.net.Inet4Address;
import java.net.UnknownHostException;

public class NodeId {

    private String nodeIdString;

    public NodeId(String nodeIdString) {
        this.nodeIdString = nodeIdString;
    }

    public NodeId(int port) throws UnknownHostException {
        this(port, Inet4Address.getLocalHost().getHostAddress());
    }

    public NodeId(int port, String hostAddress) {
        this.nodeIdString = port + ":" + hostAddress;
    }

    public String getNodeIdString() {
        return this.nodeIdString;
    }

    public int getPort() {
        return Integer.parseInt(this.nodeIdString.split(":")[0]);
    }

    public String getHostAddress() {
        return this.nodeIdString.split(":")[1];
    }
}
