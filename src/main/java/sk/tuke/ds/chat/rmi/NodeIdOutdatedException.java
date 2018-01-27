package sk.tuke.ds.chat.rmi;

public class NodeIdOutdatedException extends Exception {

    private String correctNodeId;

    public NodeIdOutdatedException(String correctNodeId) {
        this.correctNodeId = correctNodeId;
    }

    public String getCorrectNodeId() {
        return correctNodeId;
    }
}
