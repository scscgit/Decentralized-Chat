package sk.tuke.ds.chat.rmi;

import sk.tuke.ds.chat.node.NodeContext;

public class NodeIdOutdatedException extends Exception {

    private String correctNodeId;
    private NodeContext nodeContext;

    public NodeIdOutdatedException(String correctNodeId, NodeContext nodeContext) {
        this.correctNodeId = correctNodeId;
        this.nodeContext = nodeContext;
    }

    public String getCorrectNodeId() {
        return correctNodeId;
    }

    public NodeContext getNodeContext() {
        return nodeContext;
    }
}
