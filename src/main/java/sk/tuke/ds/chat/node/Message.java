package sk.tuke.ds.chat.node;

import java.util.Date;

public class Message {

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
}
