package sk.tuke.ds.chat.node;

import sk.tuke.ds.chat.rmi.ChatNodeServer;
import sk.tuke.ds.chat.rmi.abstraction.AbstractProcess;
import sk.tuke.ds.chat.util.Log;

import java.util.ArrayList;
import java.util.List;

public class BlockchainProcess {

    private final AbstractProcess process;
    private List<Message> queuedMessages = new ArrayList<>();
    private List<Message> miningUnqueuedMessages = new ArrayList<>();
    private Blockchain blockchain;
    private boolean shouldCancel;

    public BlockchainProcess(ChatNodeServer chatNodeServer, Blockchain blockchain) {
        this.blockchain = blockchain;
        this.process = new AbstractProcess() {
            @Override
            public void run() {
                while (isRunning()) {
                    // Cancelling already-mined messages in a blockchain
                    for (Message miningUnqueuedMessage : miningUnqueuedMessages) {
                        for (Message queuedMessage : queuedMessages) {
                            if (miningUnqueuedMessage.shaHash().equals(queuedMessage.shaHash())) {
                                Log.d(this,
                                        "Message SHA256 " + queuedMessage.shaHash()
                                                + " is no longer required to be mined");
                                miningUnqueuedMessages.remove(miningUnqueuedMessage);
                                queuedMessages.remove(queuedMessage);
                            }
                        }
                    }

                    // Preparing messages
                    List<Message> messages = null;
                    synchronized (this) {
                        if (!queuedMessages.isEmpty()) {
                            messages = queuedMessages;
                            // After copying the instance of ArrayList, previous reference mustn't communicate with it
                            queuedMessages = new ArrayList<>();
                        }
                    }
                    // Mining a block for messages
                    if (messages != null) {
                        Log.d(this, "Blockchain process is processing " + messages.size() + " messages");
                        Block block = new Block(blockchain.lastBlock(), messages).mineBlock(0, () -> shouldCancel);
                        shouldCancel = false;
                        if (block == null) {
                            Log.i(this, "Cancelled mining block with " + block.getMessages().size() + " messages");
                            queuedMessages.addAll(messages);
                        } else if (!blockchain.addToBlockchain(block)) {
                            Log.i(this,
                                    "Failed to add an own mined block with "
                                            + block.getMessages().size()
                                            + " messages, SHA256 "
                                            + block.shaHash());
                            queuedMessages.addAll(messages);
                        } else {
                            Log.i(this,
                                    "Mined and added to blockchain block with SHA256 "
                                            + block.shaHash()
                                            + ", announcing...");
                            chatNodeServer.announceBlock(block);
                        }
                    }
                    // Waiting for next iteration
                    Log.d(this, "Blockchain process ready...");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        this.process.start();
    }

    public synchronized void addMessage(Message message) {
        queuedMessages.add(message);
    }

    public synchronized void removeMinedMessage(Message message) {
        miningUnqueuedMessages.add(message);
    }

    public void stop() {
        this.process.stop();
    }

    public void cancel() {
        this.shouldCancel = true;
    }
}
