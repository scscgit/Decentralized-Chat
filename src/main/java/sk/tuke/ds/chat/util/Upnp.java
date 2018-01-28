package sk.tuke.ds.chat.util;

import org.fourthline.cling.UpnpServiceImpl;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.types.UDAServiceId;
import org.fourthline.cling.registry.DefaultRegistryListener;
import org.fourthline.cling.registry.Registry;
import org.fourthline.cling.support.igd.PortMappingListener;
import org.fourthline.cling.support.igd.callback.GetExternalIP;
import org.fourthline.cling.support.model.PortMapping;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * NOTE: This implementation doesn't seem to fully work and is experimental only.
 */
public class Upnp {

    public static class UpnpPayload {

        private final UpnpServiceImpl upnpService;
        private final String externalAddress;

        public UpnpPayload(UpnpServiceImpl upnpService, String externalAddress) {
            this.upnpService = upnpService;
            this.externalAddress = externalAddress;
        }

        public UpnpServiceImpl getUpnpService() {
            return upnpService;
        }

        public String getExternalAddress() {
            return externalAddress;
        }
    }

    public static class GetExternalIPResult extends GetExternalIP {

        private String externalIp;

        public GetExternalIPResult(Service service) {
            super(service);
        }

        @Override
        protected void success(String externalIPAddress) {
            Log.d(this, "Found external IP " + externalIPAddress + " via UPnP");
            externalIp = externalIPAddress;
        }

        @Override
        public void failure(ActionInvocation invocation,
                            UpnpResponse operation,
                            String defaultMsg) {
            // Something is wrong
            Log.e(Util.class, "Couldn't find an external IP via UPnP");
        }

        public String getExternalIp() {
            return externalIp;
        }
    }

    public static UpnpPayload startUpnpService(int port) throws UnknownHostException {
        PortMapping[] arr = new PortMapping[]{
                new PortMapping(port, InetAddress.getLocalHost().getHostAddress(), PortMapping.Protocol.TCP, "Decentralized Chat client (TCP)"),
                new PortMapping(port, InetAddress.getLocalHost().getHostAddress(), PortMapping.Protocol.UDP, "Decentralized Chat client (UDP)")
        };

        UpnpServiceImpl upnpService = new UpnpServiceImpl(new PortMappingListener(arr));

        List<Device> devices = new ArrayList<>();
        // Add a listener for device registration events
        upnpService.getRegistry().addListener(
                new DefaultRegistryListener() {
                    @Override
                    public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
                        devices.add(device);
                    }
                }
        );

        upnpService.getControlPoint().search();

        for (int i = 0; i < 10; i++) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            for (Device device : devices) {
                Service service = device.findService(new UDAServiceId("WANIPConnection"));
                if (service == null) {
                    continue;
                }
                Future future = upnpService.getControlPoint().execute(
                        new GetExternalIPResult(service)
                );
                GetExternalIPResult result = null;
                try {
                    result = (GetExternalIPResult) future.get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
                if (result != null) {
                    return new UpnpPayload(upnpService, result.getExternalIp());
                }
            }
        }
        Log.e(Upnp.class, "UPnP didn't even find a public IP address");
        return new UpnpPayload(upnpService, null);
    }
}
