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
    private boolean shouldCancel;

    public BlockchainProcess(ChatNodeServer chatNodeServer, Blockchain blockchain) {
        this.process = new AbstractProcess() {
            @Override
            public void run() {
                while (isRunning()) {
                    // Don't work on blockchain until the chat tab is initialized from the initial messages
                    if (chatNodeServer.getChatTab() == null || !chatNodeServer.getChatTab().isInitialized()) {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Log.d(BlockchainProcess.this, "Waiting for ChatTab initialization");
                        continue;
                    }
                    // Cancelling already-mined messages in a blockchain
                    synchronized (BlockchainProcess.this) {
                        for (Message miningUnqueuedMessage : new ArrayList<>(miningUnqueuedMessages)) {
                            for (Message queuedMessage : new ArrayList<>(queuedMessages)) {
                                if (miningUnqueuedMessage.shaHash().equals(queuedMessage.shaHash())) {
                                    Log.d(this,
                                            "Message SHA256 " + queuedMessage.shaHash()
                                                    + " is no longer required to be mined");
                                    miningUnqueuedMessages.remove(miningUnqueuedMessage);
                                    queuedMessages.remove(queuedMessage);
                                }
                            }
                        }
                    }

                    // Preparing messages
                    List<Message> messages = null;
                    synchronized (BlockchainProcess.this) {
                        if (!queuedMessages.isEmpty()) {
                            messages = queuedMessages;
                            // After copying the instance of ArrayList, previous reference mustn't communicate with it
                            queuedMessages = new ArrayList<>();
                        }
                    }
                    // Mining a block for messages
                    if (messages != null) {
                        Log.d(BlockchainProcess.this, "Blockchain process is processing " + messages.size() + " messages");
                        Block block = new Block(blockchain.lastBlock(), messages).mineBlock(0, () -> shouldCancel);
                        if (block == null) {
                            Log.i(BlockchainProcess.this,
                                    "Cancelled mining block with " + messages.size() + " messages");
                            synchronized (BlockchainProcess.this) {
                                queuedMessages.addAll(messages);
                            }
                        } else {
                            List<Message> duplicateMessages = blockchain.addToBlockchain(block);
                            if (duplicateMessages == null) {
                                Log.i(BlockchainProcess.this,
                                        "Mined and added to blockchain block number #"
                                                + blockchain.lastBlockIndex() + " with "
                                                + block.getMessages().size() + " messages of SHA256 "
                                                + block.shaHash() + ", announcing...");
                                chatNodeServer.announceBlock(block);
                            } else {
                                Log.i(BlockchainProcess.this,
                                        "Failed to add an own mined block with " + block.getMessages().size()
                                                + " messages, " + duplicateMessages.size() + " duplicates, SHA256 "
                                                + block.shaHash());
                                messages.removeAll(duplicateMessages);
                                synchronized (BlockchainProcess.this) {
                                    queuedMessages.addAll(messages);
                                }
                            }
                        }
                    }
                    // Waiting for next iteration
                    Log.d(BlockchainProcess.this, "Blockchain process ready...");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    // Cancellation is only valid if it happens during the process iteration
                    shouldCancel = false;
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
