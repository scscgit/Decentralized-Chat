package sk.tuke.ds.chat.messaging;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

public class PrivateMessage implements Serializable {

    private Date date;
    private String fromUser;
    private String toUser;
    private String message;

    public PrivateMessage(Date date, String fromUser, String toUser, String message) {
        this.date = date;
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.message = message;
    }

    public Date getDate() {
        return date;
    }

    public String getFromUser() {
        return fromUser;
    }

    public void setFromUser(String fromUser) {
        this.fromUser = fromUser;
    }

    public String getToUser() {
        return toUser;
    }

    public void setToUser(String toUser) {
        this.toUser = toUser;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public boolean equals(Object o) {
        // Same messages cannot appear at the same Date; this equality is used to filter out already received messages
        if (this == o) return true;
        if (!(o instanceof PrivateMessage)) return false;
        PrivateMessage that = (PrivateMessage) o;
        return Objects.equals(date, that.date) &&
                Objects.equals(fromUser, that.fromUser) &&
                Objects.equals(toUser, that.toUser) &&
                Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, fromUser, toUser, message);
    }
}
