package sk.tuke.ds.chat.rmi.abstraction;

import sk.tuke.ds.chat.util.Log;

import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;
import java.rmi.server.UnicastRemoteObject;

/**
 * Created by Steve on 26.09.2017.
 */
public abstract class AbstractServer implements Remote {

    private int port;
    private String registryName;
    private boolean stopped;

    /**
     * Creates RMI registry on specified port, exports the current server object and binds it to the registry.
     *
     * @param registryName the name to associate with the remote reference in the registry
     * @param port         specified port
     * @throws RemoteException
     */
    public AbstractServer(String registryName, int port) throws RemoteException {
        this.registryName = registryName;
        this.port = port;
        try {
            UnicastRemoteObject.unexportObject(this, false);
        } catch (Exception e) {
            // Everything is okay if not exported yet
        }
        try {
            Remote stub = UnicastRemoteObject.exportObject(this, port);
            Registry registry;
            try {
                // Registry has to be created before being available
                registry = LocateRegistry.createRegistry(port);
            } catch (ExportException e) {
                // Registry was already created, existing one has to be reused
                registry = LocateRegistry.getRegistry(port);
            }
            registry.bind(registryName, stub);
            Log.i(this, "bound");
        } catch (Exception e) {
            Log.e(this, e);
        }
    }

    /**
     * Unbinds the current server object from the RMI registry and unexports it.
     *
     * @throws RemoteException
     */
    public void stop() throws RemoteException {
        Registry registry = LocateRegistry.getRegistry(port);
        try {
            registry.unbind(this.registryName);
        } catch (NotBoundException e) {
            e.printStackTrace(System.err);
            throw new RemoteException(
                    getClass().getName() + " Registry was not bound by being started: " + e.getMessage());
        }
        UnicastRemoteObject.unexportObject(this, true);
        Log.i(this, "unbound");
        stopped = true;
    }

    protected int getPort() {
        return this.port;
    }

    public boolean isStopped() {
        return stopped;
    }
}
