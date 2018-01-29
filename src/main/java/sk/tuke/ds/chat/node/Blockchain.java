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
                                new Message(new Date(), "System", "Initiated new chat session (empty blockchain)")
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
        // The logic to decide which blockchain will survive intact (hint: it's the longer one, otherwise compare hash)
        int shortestBlockchainLastIndex;
        boolean thisShortest;
        if (lastBlockIndex() == otherBlockchain.lastBlockIndex()) {
            thisShortest = otherBlockchain.lastBlock().shaHash().compareTo(lastBlock().shaHash()) > 0;
            shortestBlockchainLastIndex = thisShortest ? lastBlockIndex() : otherBlockchain.lastBlockIndex();
        } else {
            shortestBlockchainLastIndex = Math.min(lastBlockIndex(), otherBlockchain.lastBlockIndex());
            thisShortest = lastBlockIndex() == shortestBlockchainLastIndex;
        }

        // Conflict resolution if genesis block matches
        for (int blockchainIndex = shortestBlockchainLastIndex;
             blockchainIndex >= 0;
             blockchainIndex--
                ) {
            Block myBlock = this.chain.get(blockchainIndex);
            Block otherBlock = otherBlockchain.chain.get(blockchainIndex);
            if (myBlock.shaHash().equals(otherBlock.shaHash())) {
                // Joined blockchains; finding orphaned messages
                Log.i(this,
                        "[Blockchain conflict resolution] Diverged after our chain's block "
                                + blockchainIndex + "/" + lastBlockIndex()
                                + " of SHA256: " + this.chain.get(blockchainIndex).shaHash());
                if (thisShortest) {
                    // Swap blockchains and completely reset the chat
                    List<Block> orphanedBlocks = this.chain;
                    this.chain = otherBlockchain.chain;
                    if (lastBlockIndex() == blockchainIndex) {
                        Log.i(this, "Special case of blockchain incompatibility, " +
                                "where the shortest chain doesn't diverge at all");
                    }
                    resetDisplayedMessages(orphanedBlocks, blockchainIndex + 1, thisServer);
                } else {
                    joinBlockchainAfter(
                            blockchainIndex + 1,
                            this.chain,
                            otherBlockchain.chain,
                            thisServer
                    );
                }
                return true;
            }
        }
        //Following is true: !this.chain.get(0).shaHash().equals(otherBlockchain.chain.get(0).shaHash())
        Log.e(this,
                "Blockchains totally incompatible in <" + thisServer.getNodeId().getUsername()
                        + ">; replaced by " + (thisShortest ? "other" : "our own (no change locally, " +
                        "assuming other will re-announce its messages)"));
        if (thisShortest) {
            // Swap blockchains and completely reset the chat
            List<Block> orphanedBlocks = this.chain;
            this.chain = otherBlockchain.chain;
            resetDisplayedMessages(orphanedBlocks, 0, thisServer);
        }
        return true;
    }

    private void resetDisplayedMessages(
            List<Block> orphanedBlocks,
            int orphanBlockchainRecoveryStartIndex,
            ChatNodeServer thisServer) {
        thisServer.getChatTab().clearMessages();
        // First spam all the private messages
        thisServer.getPrivateMemory().displayAll(thisServer);
        // Then the actual shared messages
        this.chain.stream().flatMap(block -> block.getMessages().stream()).forEach(
                message -> thisServer.getChatTab().addMessage(
                        message.getUser(),
                        new String[]{message.getMessage()},
                        message.getDate()
                )
        );
        // Now restore what remains of the old blockchain by mining it in a block in a cooperative way
        orphanedBlocks
                .subList(orphanBlockchainRecoveryStartIndex, orphanedBlocks.size())
                .stream()
                .flatMap(block -> block.getMessages().stream())
                .forEach(thisServer::announceMessage);
    }

    /**
     * Copies blocks from blockchainIndex (they are supposed to be all diverged) of oldBlockchain into a newBlockchain;
     * that is, only picking unique Messages missing in newBlockchain and announcing the missing messages to be mined.
     */
    private void joinBlockchainAfter(int blockchainIndexStart, List<Block> orphanedBlocks, List<Block> newBlocks, ChatNodeServer thisServer) {
        List<Message> orphanMessages = orphanedBlocks
                .subList(blockchainIndexStart, orphanedBlocks.size())
                .stream()
                .flatMap(block -> block.getMessages().stream())
                .collect(Collectors.toList());
        // Removing duplicate orphans that are already in the other blockchain
        newBlocks.subList(blockchainIndexStart, newBlocks.size())
                .stream()
                .flatMap(block -> block.getMessages().stream())
                .forEach(
                        otherMessage -> orphanMessages.removeIf(
                                orphanMessage -> orphanMessage.shaHash().equals(otherMessage.shaHash())
                        )
                );
        Log.i(this,
                "[Blockchain conflict resolution] " +
                        "Migrated <" + thisServer.getNodeId().getUsername() + "> to blockchain of length "
                        + lastBlockIndex() + " having last block SHA256: " + lastBlock().shaHash()
                        + ", returning the following orphaned messages to a mining queue:");
        // Remembering to add the old messages that were cut out back to the blockchain mining process
        for (int i = 0; i < orphanMessages.size(); i++) {
            Message orphanMessage = orphanMessages.get(i);
            Log.i(this,
                    "[Restoring orphan " + (i + 1) + "/" + orphanMessages.size() + "] "
                            + orphanMessage.getUser() + ": " + orphanMessage.getMessage());
            thisServer.announceMessage(orphanMessage);
        }
    }

    public void displayBlocksInto(Consumer<Block> consumer) {
        this.chain.forEach(consumer);
    }
}
