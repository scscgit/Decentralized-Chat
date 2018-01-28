package sk.tuke.ds.chat.util;

import org.fourthline.cling.UpnpServiceImpl;
import org.fourthline.cling.support.igd.PortMappingListener;
import org.fourthline.cling.support.model.PortMapping;
import sk.tuke.ds.chat.node.NodeId;

import java.awt.*;
import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.List;

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

    @SuppressWarnings("unchecked")
    public static <T extends Remote> T rmiTryLookup(NodeId nodeId, String serviceName) {
        try {
            return (T) LocateRegistry.getRegistry(
                    nodeId.getHostAddress(),
                    nodeId.getPort()
            ).lookup(serviceName);
        } catch (RemoteException | NotBoundException | ClassCastException e) {
            e.printStackTrace();
            Log.e(Util.class, e);
            return null;
        }
    }

    public static void savePeersConfiguration(List<String> peers, int hostPort) {
        try {
            File file = new File("peers-config-" + hostPort + ".txt");
            if (!file.exists()) {
                file.createNewFile();
            }
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            for (String peer : peers) {
                writer.write(peer + "\n");
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<String> loadPeersConfiguration(int hostPort) {
        ArrayList<String> peers = new ArrayList<>();
        try {
            File file = new File("peers-config-" + hostPort + ".txt");
            if (file.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                while (reader.ready()) {
                    peers.add(reader.readLine());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        peers.removeIf(String::isEmpty);
        return peers;
    }

    public static UpnpServiceImpl startUpnpService(int port) throws UnknownHostException {
        PortMapping[] arr = new PortMapping[]{
                new PortMapping(port, InetAddress.getLocalHost().getHostAddress(), PortMapping.Protocol.TCP, "Decentralized Chat client (TCP)"),
//                new PortMapping(port, InetAddress.getLocalHost().getHostAddress(), PortMapping.Protocol.UDP, "Decentralized Chat client (UDP)")
        };

        UpnpServiceImpl upnpService = new UpnpServiceImpl(new PortMappingListener(arr));

        upnpService.getControlPoint().search();
        return upnpService;
    }
}
