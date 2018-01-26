package sk.tuke.ds.chat;

import sk.tuke.ds.chat.layouts.ChatLayout;
import sk.tuke.ds.chat.rmi.abstraction.AbstractProcess;

public class Launcher {

    public static void main(String[] args) {
        new AbstractProcess() {
            @Override
            public void run() {
                ChatLayout.main(args);
            }
        }.start();
        ChatLayout.main(args);
    }
}
