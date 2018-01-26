package sk.tuke.ds.chat.node;

import org.apache.commons.codec.digest.DigestUtils;
import sk.tuke.ds.chat.util.Log;

import java.io.Serializable;
import java.util.List;
import java.util.function.Supplier;

/**
 * Single block is constructed in a following way:
 * <p>
 * =====================================
 * | Header:     Nonce                 |
 * |             Previous block hash   |
 * | Content:    Message 1             |
 * |             ...                   |
 * |             Message n             |
 * | Trailer:    Next block difficulty |
 * =====================================
 * <p>
 * Nonce's purpose is to validate the difficulty with previous block hash.
 * Messages' sha hashes are all joined as each separated by newline character.
 * Trailer sets a requirement for the next block.
 * <p>
 * All of these values end with a newline character (except last one) and are all hashed together as a String.
 */
public class Block implements Serializable {

    private int nonce;
    private String previousBlockHash;
    private List<Message> messages;
    private int currentDifficulty;
    private int nextDifficulty;

    public Block(Block previousBlock, List<Message> messages) {
        this.previousBlockHash = previousBlock == null ? "" : previousBlock.shaHash();
        this.messages = messages;
        this.currentDifficulty = previousBlock == null ? Blockchain.START_DIFFICULTY : previousBlock.nextDifficulty;
        // Next difficulty should recalculate
        this.nextDifficulty = this.currentDifficulty;
    }

    public boolean isHashValidForDifficulty(int difficulty) {
        StringBuilder start = new StringBuilder();
        for (int i = 0; i < difficulty; i++) {
            start.append("0");
        }
        return shaHash().substring(0, difficulty).equals(start.toString());
    }

    public Block mineBlock(int startNonce, Supplier<Boolean> stop) {
        this.nonce = startNonce;
        int i = 0;
        while (!isHashValidForDifficulty(this.currentDifficulty)) {
            i++;
            if (i > 20_000) {
                Log.d(this, "20_000 invalid hashes during mining, e.g: " + shaHash());
                i = 0;
            }
            this.nonce++;
            if (stop.get()) {
                return null;
            }
        }
        Log.d(this, "Mined block with nonce " + this.nonce + ": hash " + this.shaHash());
        return this;
    }

    public String getPreviousBlockHash() {
        return previousBlockHash;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public int getNextDifficulty() {
        return nextDifficulty;
    }

    public String shaHash() {
        StringBuilder messagesString = new StringBuilder();
        for (Message message : messages) {
            messagesString.append("\n").append(message.shaHash());
        }
        return DigestUtils.sha256Hex(
                // Header
                ("" + nonce)
                        + "\n" + previousBlockHash
                        // Content
                        + messagesString
                        // Trailer
                        + "\n" + ("" + nextDifficulty));
    }
}
