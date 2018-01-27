package sk.tuke.ds.chat.util;

import sk.tuke.ds.chat.layouts.ChatLayout;

/**
 * Created by Steve on 01.10.2017.
 */
public class Log {

    public static void d(Object context, String message) {
        System.out.println("D> " + simpleContext(context) + ": " + message);
    }

//    public static void d(Object context, String message, Object... args) {
//        System.out.println("D> " + context + ": " + String.format(message, (Object[]) args));
//    }

    public static void i(Object context, String message) {
        ChatLayout.setStatus("I> " + simpleContext(context) + ": " + message);
        System.out.println("I> " + simpleContext(context) + ": " + message);
    }

//    public static void i(Object context, String message, Object... args) {
//        System.out.println("I> " + context + ": " + String.format(message, (Object[]) args));
//    }

    public static void e(Object context, String message) {
        ChatLayout.setStatus("ERROR> " + simpleContext(context) + ": " + message);
        System.err.println("ERROR> " + simpleContext(context) + ": " + message);
    }

//    public static void e(Object context, String message, Object... args) {
//        System.err.println("ERROR> " + context + ": " + String.format(message, (Object[]) args));
//    }

    public static void e(Object context, Throwable e) {
        e(context, "exception: " + e.toString());
        e.printStackTrace(System.err);
    }

    private static String simpleContext(Object context) {
        if (context instanceof Class) {
            return ((Class<?>) context).getSimpleName();
        }
        return context.getClass().getSimpleName();
    }
}
