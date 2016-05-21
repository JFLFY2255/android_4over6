package wang.a1ex.android_4over6;

/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import java.net.Socket;

public class IVIVpnService extends VpnService implements Handler.Callback, Runnable {
    private static final String TAG = "IVIVpnService";
    private static String ServerAddress = "2402:f000:1:4417::900";
    private static final String PCAP_FILE = "packets.pcap";
    private String mServerPort = "5678";
    private Handler mHandler;
    private Thread mThread;
    private static final long StatisticsInterval = 500;
    long lastStatisticsTime = 0;
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // The handler is only used to show messages.
        if (mHandler == null) {
            mHandler = new Handler(this);
        }
        // Stop the previous session by interrupting the thread.
        if (mThread != null) {
            mThread.interrupt();
        }
        // Start a new session by creating a new thread.
        mThread = new Thread(this, "IVIVpnThread");
        mThread.start();
        return START_STICKY;
    }
    @Override
    public void onDestroy() {
        if (mThread != null) {
            mThread.interrupt();
        }
    }
    @Override
    public boolean handleMessage(Message message) {
        if (message != null) {
            if (message.obj != null) {
                String str = (String) message.obj;
                Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(this, message.what, Toast.LENGTH_SHORT).show();
            }

        }
        return true;
    }
    Pcap pcap;
    @Override
    public synchronized void run() {
        try {
            Log.i(TAG, "Starting");
            pcap = new Pcap();
            startVpnLoop();
        } catch (Exception e) {
            Log.e(TAG, "Got " + e.toString());
        }
    }

    VpnCallbacks vpnCallbacks = new VpnCallbacks() {
        @Override
        public void onHeartbeat() {
            Log.d("IVIVpnService", "on heartbeat");
        }

        @Override
        public void onStatistics(int rBytes, int rPackets, int sBytes, int sPackets) {
            if (System.currentTimeMillis() - lastStatisticsTime > StatisticsInterval) {
                lastStatisticsTime = System.currentTimeMillis();
                Intent intent = new Intent(MainActivity.BROADCAST_NAME);
                intent.putExtra(MainActivity.BROADCAST_INTENT_BYTES_SENT, sBytes);
                intent.putExtra(MainActivity.BROADCAST_INTENT_BYTES_RECEIVED, rBytes);
                intent.putExtra(MainActivity.BROADCAST_INTENT_PACKETS_SEND, sPackets);
                intent.putExtra(MainActivity.BROADCAST_INTENT_PACKETS_RECEIVED, rPackets);
                sendBroadcast(intent);
            }
        }

        @Override
        public int onReceiveDhcpAndCreateTun(String dhcpString, int pipeFd) {
            //Socket socket =
            return configure(dhcpString);
        }

        @Override
        public void onPacketReceived(int length, byte type, byte[] packet) {
            Log.d("IVIVpnService", "onPacketReceived " + String.valueOf(length));
            pcap.addPacket(packet);
            pcap.saveToSDCardFile("vpn.pcap");
        }

        @Override
        public void onPacketSent(int length, byte type, byte[] packet) {
            Log.d("IVIVpnService", "onPacketSent " + String.valueOf(length));
            pcap.addPacket(packet);
            pcap.saveToSDCardFile("vpn.pcap");
        }
    };

    private void startVpnLoop() {
        while (true) {
            VpnDevices vpnDevices = new VpnDevices(vpnCallbacks);
            vpnDevices.startVpn();
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private int configure(String parameters) {
        // Configure a builder while parsing the parameters.
        Builder builder = new Builder();
        String[] parameterArray = parameters.split(" ");
        if (parameterArray.length >= 5) {
            String ip = parameterArray[0];
            String route = parameterArray[1];
            builder.setMtu(1500);
            builder.addAddress(ip, 24);
            builder.addRoute("0.0.0.0", 0);
            builder.addDnsServer("166.111.8.28");
            builder.setBlocking(true);
            try {
                builder.addDisallowedApplication("wang.a1ex.android_4over6");
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            //for (int i = 0; i < 3; ++i)
            //    builder.addDnsServer(parameterArray[2 + i]);
        }
        // Create a new interface using the builder and save the parameters.
        ParcelFileDescriptor tunIf = builder.setSession(ServerAddress).establish();
        Log.i(TAG, "New interface: " + parameters);
        return tunIf.getFd();
    }
}