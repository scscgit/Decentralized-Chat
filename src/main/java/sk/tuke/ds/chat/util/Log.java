package sk.tuke.ds.chat.util;

/**
 * Created by Steve on 01.10.2017.
 */
public class Log {

    public static void d(Object context, String message) {
        System.out.println("D> " + context + ": " + message);
    }

    public static void d(Object context, String message, Object... args) {
        System.out.println("D> " + context + ": " + String.format(message, (Object[]) args));
    }

    public static void i(Object context, String message) {
        System.out.println("I> " + context + ": " + message);
    }

    public static void i(Object context, String message, Object... args) {
        System.out.println("I> " + context + ": " + String.format(message, (Object[]) args));
    }

    public static void e(Object context, String message) {
        System.err.println("ERROR> " + context + ": " + message);
    }

    public static void e(Object context, String message, Object... args) {
        System.err.println("ERROR> " + context + ": " + String.format(message, (Object[]) args));
    }

    public static void e(Object context, Throwable e) {
        e(context, "exception:");
        e.printStackTrace(System.err);
    }
}
