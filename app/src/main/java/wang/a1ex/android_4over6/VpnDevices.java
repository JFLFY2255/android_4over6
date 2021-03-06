package wang.a1ex.android_4over6;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

public class VpnDevices {
    static {
        System.loadLibrary("VpnDevices");
    }

    private VpnCallbacks vpnCallbacks;
    private int jcPort;
    private int reconnect;
    public VpnDevices(VpnCallbacks vpnCallbacks, int jcPort, int reconnect) {
        this.jcPort = jcPort;
        this.vpnCallbacks = vpnCallbacks;
        this.reconnect = reconnect;
    }

    public native int startVpn();
}
