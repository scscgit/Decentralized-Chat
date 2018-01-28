package sk.tuke.ds.chat.messaging;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.Serializable;
import java.util.Date;

public class Message implements Serializable {

    private Date date;
    private String user;
    private String message;

    public Message(Date date, String user, String message) {
        this.date = date;
        this.user = user;
        this.message = message;
    }

    public Date getDate() {
        return date;
    }

    public String getUser() {
        return user;
    }

    public String getMessage() {
        return message;
    }

    public String shaHash() {
        return DigestUtils.sha256Hex(date.toString() + "\n" + user + "\n" + message);
    }
}
