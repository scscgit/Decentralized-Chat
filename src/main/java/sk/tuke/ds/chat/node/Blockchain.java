package sk.tuke.ds.chat.node;

import sk.tuke.ds.chat.util.Log;

import java.io.Serializable;
import java.util.*;
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

    public Block lastBlock() {
        return this.chain.get(this.chain.size() - 1);
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

    public synchronized boolean joinBlockchain(Blockchain otherBlockchain) {
        if (lastBlock().shaHash().equals(otherBlockchain.lastBlock().shaHash())) {
            // Blockchains are OK
            return false;
        }
        // Conflict resolution
        for (int myBlockchainIndex = this.chain.size() - 1;
             myBlockchainIndex > 0;
             myBlockchainIndex--
                ) {
            Block myBlock = this.chain.get(myBlockchainIndex);
            for (int otherBlockchainIndex = otherBlockchain.chain.size() - 1;
                 otherBlockchainIndex > 0;
                 otherBlockchainIndex--
                    ) {
                Block otherBlock = otherBlockchain.chain.get(otherBlockchainIndex);
                if (myBlock.shaHash().equals(otherBlock.shaHash())) {
                    // Joined blockchains; finding orphaned messages
                    Log.i(this,
                            "[Blockchain conflict resolution] Joined at our own chain's block "
                                    + myBlockchainIndex + "/" + (this.chain.size() - 1)
                                    + ", SHA256: " + this.chain.get(myBlockchainIndex).shaHash());
                    List<Block> orphans = this.chain.subList(myBlockchainIndex + 1, this.chain.size());
                    List<Message> orphanMessages = orphans
                            .stream()
                            .flatMap(block -> block.getMessages().stream())
                            .collect(Collectors.toList());
                    // Removing duplicate orphans that are already in the other blockchain
                    otherBlockchain.chain.subList(otherBlockchainIndex + 1, otherBlockchain.chain.size())
                            .stream()
                            .flatMap(block -> block.getMessages().stream())
                            .forEach(otherMessage -> orphanMessages.removeIf(
                                    orphanMessage -> {
                                        Log.i(this,
                                                "[Blockchain conflict resolution] " +
                                                        "Removed duplicate message " +
                                                        orphanMessage.getUser() + ": " + orphanMessage.getMessage());
                                        return orphanMessage.shaHash().equals(otherMessage.shaHash());
                                    }
                                    )
                            );
                    // Using other blockchain
                    this.chain = otherBlockchain.chain;
                    // Remembering to add the old transactions back
                }
            }
        }
        return true;
    }
}
