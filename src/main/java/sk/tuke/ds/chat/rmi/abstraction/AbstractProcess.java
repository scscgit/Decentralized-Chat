package sk.tuke.ds.chat.rmi.abstraction;

/**
 * Created by Steve on 26.09.2017.
 */
public abstract class AbstractProcess implements Runnable {

    private Thread thread;
    private boolean stopFlag;

    public void start() {
        thread = new Thread(this);
//        thread = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                AbstractProcess.this.run();
//            }
//        });
        thread.start();
    }

    public void stop() {
        this.stopFlag = true;
    }

    public boolean isRunning() {
        return !this.stopFlag;
    }

    @Override
    public abstract void run();

    public void join() throws InterruptedException {
        thread.join();
    }
}

