package sk.tuke.ds.chat.node;

import java.net.Inet4Address;
import java.net.UnknownHostException;

public class NodeId {

    private String nodeIdString;

    public NodeId(String nodeIdString) {
        this.nodeIdString = nodeIdString;
    }

    public NodeId(int port, String username) throws UnknownHostException {
        this(port, Inet4Address.getLocalHost().getHostAddress(), username);
    }

    public NodeId(int port, String hostAddress, String username) {
        set(port, hostAddress, username);
    }

    private void set(int port, String hostAddress, String username) {
        this.nodeIdString = port + ":" + hostAddress + ":" + username;
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

    public String getUsername() {
        return this.nodeIdString.split(":")[2];
    }

    public void setUsername(String username) {
        set(getPort(), getHostAddress(), username);
    }
}
