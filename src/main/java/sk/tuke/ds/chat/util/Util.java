package sk.tuke.ds.chat.util;

import java.awt.*;
import java.io.*;

public class Util {

    public static <T extends Serializable> T copySerializable(T serializable) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)
        ) {
            objectOutputStream.writeObject(serializable);
            try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
                 ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)
            ) {
                return (T) objectInputStream.readObject();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static <T extends Component> T findComponentIn(Container inContainer, String name) {
        if (name == null || "".equals(name)) {
            throw new RuntimeException("No name for finding components provided");
        }
        T componentInRecursively = findComponentInRecursively(inContainer, name);
        if (componentInRecursively != null) {
            return componentInRecursively;
        }
        throw new RuntimeException("Didn't find component named " + name + " in " + inContainer.getName());
    }

    private static <T extends Component> T findComponentInRecursively(Container inContainer, String name) {
        for (Component component : inContainer.getComponents()) {
            if (name.equals(component.getName())) {
                return (T) component;
            }
            if (component instanceof Container) {
                T componentInRecursively = findComponentInRecursively(((Container) component), name);
                if (componentInRecursively != null) {
                    return componentInRecursively;
                }
            }
        }
        return null;
    }
}
