package sk.tuke.ds.chat.node;

import sk.tuke.ds.chat.messaging.Message;
import sk.tuke.ds.chat.rmi.ChatNodeServer;
import sk.tuke.ds.chat.util.Log;

import java.io.Serializable;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class Blockchain implements Serializable {

    public static final int START_DIFFICULTY = 4;

    private List<Block> chain;

    public Blockchain() {
        // Genesis block
        this.chain = new ArrayList<>();
        this.chain.add(
                new Block(
                        null,
                        Collections.singletonList(
                                new Message(new Date(), "System", "Initiated new chat session")
                        )
                ).mineBlock(0, () -> false)
        );
    }

    public Blockchain(List<Block> chain) {
        this.chain = chain;
    }

    public int lastBlockIndex() {
        return this.chain.size() - 1;
    }

    public Block lastBlock() {
        return this.chain.get(lastBlockIndex());
    }

    /**
     * @return null if successful, Messages if they are duplicates and should be removed from the queue
     */
    public synchronized List<Message> addToBlockchain(Block block) {
        Block lastBlock = lastBlock();
        if (lastBlock.shaHash().equals(block.getPreviousBlockHash())
                && block.isHashValidForDifficulty(lastBlock.getNextDifficulty())) {

            // Ad-hoc solution for "duplicate prevention" - the same message cannot occur in 5 following blocks
            // This should instead be solved via Date of the message, which should be in the block interval
            ArrayList<Message> duplicateMessages = new ArrayList<>();
            for (int i = this.chain.size() - 1; i > this.chain.size() - 6 && i > 0; i--) {
                for (int j = 0; j < block.getMessages().size(); j++) {
                    Message messageToAssertNonDuplicate = block.getMessages().get(j);
                    if (this.chain.get(i).getMessages().stream().anyMatch(
                            message -> message.shaHash().equals(messageToAssertNonDuplicate.shaHash())
                    )) {
                        Log.e(this,
                                "Attempted to add a duplicate message, which was already in a blockchain");
                        duplicateMessages.add(messageToAssertNonDuplicate);
                    }
                }
            }
            if (!duplicateMessages.isEmpty()) {
                return duplicateMessages;
            }

            this.chain.add(block);
            return null;
        }
        return new ArrayList<>();
    }

    public void addSingleMessageBlockByMining(Message message) {
        Block block;
        do {
            Log.i(this, "Mining a single-message block (for message " + message.getMessage() + ")...");
            block = new Block(lastBlock(), Collections.singletonList(message));
            block.mineBlock(new Random().nextInt(), () -> false);
        } while (addToBlockchain(block) != null);
    }

    public int getDifficulty() {
        // This should scale with users (timestamp differential against expected target block duration)
        return lastBlock().getNextDifficulty();
    }

    public boolean isValid() {
        Block previousBlock = this.chain.get(0);
        for (int i = 1; i < this.chain.size() - 1; i++) {
            Block block = this.chain.get(i);
            if (!block.getPreviousBlockHash().equals(previousBlock.shaHash())
                    || !block.isHashValidForDifficulty(previousBlock.getNextDifficulty())) {
                return false;
            }
        }
        return true;
    }

    public synchronized boolean joinBlockchain(Blockchain otherBlockchain, ChatNodeServer thisServer) {
        if (lastBlock().shaHash().equals(otherBlockchain.lastBlock().shaHash())) {
            // Blockchains are OK
            return false;
        }
        int shortestBlockchainLastIndex = Math.min(lastBlockIndex(), otherBlockchain.lastBlockIndex());
        boolean thisShortest = lastBlockIndex() == shortestBlockchainLastIndex;

        // Conflict resolution
        for (int blockchainIndex = shortestBlockchainLastIndex;
             blockchainIndex > 0;
             blockchainIndex--
                ) {
            Block myBlock = this.chain.get(blockchainIndex);
            Block otherBlock = otherBlockchain.chain.get(blockchainIndex);
            if (!myBlock.shaHash().equals(otherBlock.shaHash())) {
                // Joined blockchains; finding orphaned messages
                Log.i(this,
                        "[Blockchain conflict resolution] Diverged after our chain's block "
                                + blockchainIndex + "/" + lastBlockIndex()
                                + " of SHA256: " + this.chain.get(blockchainIndex).shaHash());
                joinBlockchainAfter(
                        blockchainIndex,
                        thisShortest ? this : otherBlockchain,
                        thisShortest ? otherBlockchain : this,
                        thisServer
                );
            }
        }
        return true;
    }

    private void joinBlockchainAfter(int blockchainIndex, Blockchain oldBlockchain, Blockchain newBlockchain, ChatNodeServer thisServer) {
        List<Block> orphans = oldBlockchain.chain.subList(blockchainIndex + 1, oldBlockchain.lastBlockIndex() + 1);
        List<Message> orphanMessages = orphans
                .stream()
                .flatMap(block -> block.getMessages().stream())
                .collect(Collectors.toList());
        // Removing duplicate orphans that are already in the other blockchain
        newBlockchain.chain.subList(blockchainIndex + 1, newBlockchain.lastBlockIndex() + 1)
                .stream()
                .flatMap(block -> block.getMessages().stream())
                .forEach(
                        otherMessage -> orphanMessages.removeIf(
                                orphanMessage -> orphanMessage.shaHash().equals(otherMessage.shaHash())
                        )
                );
        // Using other blockchain
        this.chain = newBlockchain.chain;
        Log.i(this,
                "[Blockchain conflict resolution] " +
                        "Migrated to blockchain of length " + lastBlockIndex()
                        + " having last block SHA256: " + lastBlock().shaHash()
                        + ", returning the following orphaned messages to a mining queue:");
        // Remembering to add the old messages that were cut out back to the blockchain mining process
        for (int i = 0; i < orphanMessages.size(); i++) {
            Message orphanMessage = orphanMessages.get(i);
            Log.i(this,
                    "[Restoring orphan " + (i + 1) + "/" + (orphanMessages.size() - 1) + "] "
                            + orphanMessage.getUser() + ": " + orphanMessage.getMessage());
            thisServer.receiveAnnouncedMessage(orphanMessage);
        }
    }

    public void displayBlocksInto(Consumer<Block> consumer) {
        this.chain.forEach(consumer);
    }
}
