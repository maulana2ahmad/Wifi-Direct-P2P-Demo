package com.redudant.wifidirectp2pdemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.nfc.cardemulation.HostApduService;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    Button btnonOff, btndiscover, btnsendButton;
    ListView peerListView;
    TextView read_Msg, connectionStatus;
    EditText writ_eMsg;

    WifiManager wifiManager;
    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;
    BroadcastReceiver mReceiver;
    IntentFilter mIntentFilter;

    //Connect to peer
    List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    String[] devicesNameArray;
    WifiP2pDevice[] devicesArray;

    //massage
    final static int MESSAGE_READ = 1;

    //
    ServerClass serverClass;
    ClientClass clientClass;
    SendReceive sendReceive;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initialWork();
        exqListener();
    }

    //handler masange
    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_READ:
                    byte[] readBuff = (byte[]) msg.obj;
                    String tempMsg = new String(readBuff, 0, msg.arg1);
                    read_Msg.setText(tempMsg);
                    break;
            }

            return true;
        }
    });
    //end handler masange

    //method onClic kbtn
    private void exqListener() {

        btnonOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Log.d( "TEST", String.valueOf(v)); //ceking error

                if (wifiManager.isWifiEnabled()) {

                    wifiManager.setWifiEnabled(false);
                    btnonOff.setText("ON");
                } else {

                    wifiManager.setWifiEnabled(true);
                    btnonOff.setText("OFF");
                }

            }
        });

        btndiscover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //menemukan devices
                mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {

                        connectionStatus.setText("Discoverry Started");
                    }

                    @Override
                    public void onFailure(int reason) {

                        connectionStatus.setText("Discoverry Started Failed");
                    }
                });
            }
        });


        peerListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                //menyambungkan ke device
                final WifiP2pDevice device = devicesArray[position];

                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device.deviceAddress;
                mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {

                        Toast.makeText(getApplicationContext(), "Connected to " + device.deviceName, Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onFailure(int reason) {

                        Toast.makeText(getApplicationContext(), "Not Connected ", Toast.LENGTH_LONG).show();

                    }
                });
            }
        });

        //button send
        btnsendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String msg = writ_eMsg.getText().toString();
                sendReceive.write(msg.getBytes());
            }
        });

    }

    //method initialitation
    public void initialWork() {

        btnonOff = (Button) findViewById(R.id.onOff);
        btndiscover = (Button) findViewById(R.id.discover);
        btnsendButton = (Button) findViewById(R.id.sendButton);
        peerListView = (ListView) findViewById(R.id.peerListView);
        read_Msg = (TextView) findViewById(R.id.readMsg);
        connectionStatus = (TextView) findViewById(R.id.connectionStatus);
        writ_eMsg = (EditText) findViewById(R.id.writeMsg);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        mReceiver = new WiFiDirectBroadcastReceiver(mManager, mChannel, this);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

    }

    //menacai device di set ke Array
    WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peersList) {

            if (!peersList.getDeviceList().equals(peers)) {

                peers.clear();
                peers.addAll(peersList.getDeviceList());

                devicesNameArray = new String[peersList.getDeviceList().size()];
                devicesArray = new WifiP2pDevice[peersList.getDeviceList().size()];
                int indext = 0;

                for (WifiP2pDevice device : peersList.getDeviceList()) {
                    devicesNameArray[indext] = device.deviceName;
                    devicesArray[indext] = device;
                    indext++;
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, devicesNameArray);
                peerListView.setAdapter(adapter);
            }

            if (peers.size() == 0) {
                Toast.makeText(getApplicationContext(), "No Devices Found", Toast.LENGTH_LONG).show();
                return;
            }
        }
    };

    //connectionListener dari devices config
    WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo info) {

            final InetAddress groupOwnerAddress = info.groupOwnerAddress;

            if (info.groupFormed && info.isGroupOwner) {

                connectionStatus.setText("Host");

                //get object serverClass
                serverClass = new ServerClass();
                serverClass.start();

            } else if (info.groupFormed) {
                connectionStatus.setText("Client");

                //get object serverClass
                clientClass = new ClientClass(groupOwnerAddress);
                clientClass.start();
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();

        registerReceiver(mReceiver, mIntentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();

        unregisterReceiver(mReceiver);
    }

    //transfer data Server
    public class ServerClass extends Thread {

        Socket socket;
        ServerSocket serverSocket;

        //ctr + O
        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(8888);
                socket = serverSocket.accept();

                //get object sendReceive
                sendReceive = new SendReceive(socket);
                sendReceive.start();

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
    //end transfer data

    //send
    private class SendReceive extends Thread {
        private Socket socket;
        private InputStream inputStream;
        private OutputStream outputStream;

        //constructot
        public SendReceive(Socket skt) {
            socket = skt;

            try {

                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //ctr + O
        @Override
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (socket != null) {
                try {
                    bytes = inputStream.read(buffer);

                    if (bytes > 0) {
                        handler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        //write
        public void write(byte[] bytes) {

            try {

                outputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // client server
    public class ClientClass extends Thread {

        Socket socket;
        String hostAdd;

        public ClientClass(InetAddress hostAddress) {

            hostAdd = hostAddress.getHostAddress();
            socket = new Socket();
        }

        //ctr + O
        @Override
        public void run() {
            try {
                socket.connect(new InetSocketAddress(hostAdd, 8888), 500);

                //get object SendReceive
                sendReceive = new SendReceive(socket);
                sendReceive.start();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    //end client server
}
