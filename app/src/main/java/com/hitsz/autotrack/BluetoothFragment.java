package com.hitsz.autotrack;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

public class BluetoothFragment extends Fragment {

    public List<String> msglist;
    public List<BluetoothDevice> devicelist;
    public List<String> devices;

    public static final int REQUEST_BT_ENABLE_CODE = 200;
    public static final String BT_UUID = "00001101-0000-1000-8000-00805F9B34FB";//uuid

    private BluetoothAdapter mBluetoothAdapter;//蓝牙适配器
    public ConnectThread mConnectThread; //客户端线程
    private AcceptThread mAcceptThread; //服务端线程

    public ListView listView_device;
    public ListView listView_msg;

    EditText inputEt;
    Button openBt;
    Button closeBt;
    Button startBt;
    Button stopBt;
    Button sendBt;

    private Handler mHandler = new Handler() {
        @Override
        public void dispatchMessage(Message msg) {
            if(msg.what == Params.DETECT_CONNECT){
                if(mConnectThread == null){
                    //Toast.makeText(getActivity(), "没有连接蓝牙设备！", Toast.LENGTH_SHORT).show();
                }else {
                    mConnectThread.write((String) msg.obj);
                    //addMessage((String) msg.obj);
                }
            }else {
                addMessage((String) msg.obj);
            }
        }
    };

    public Handler getmHandler() {
        return mHandler;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bluetooth_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

        msglist = new ArrayList<>();
        devicelist = new ArrayList<>();
        devices = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
        }

        inputEt = (EditText) view.findViewById(R.id.input);
        openBt = (Button) view.findViewById(R.id.open);
        openBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openBT();
                addMessage("打开蓝牙");
                if (mAcceptThread == null && mBluetoothAdapter != null) {
                    mAcceptThread = new AcceptThread();
                    mAcceptThread.start();
                    addMessage("启动服务线程");
                }
            }
        });
        closeBt = (Button) view.findViewById(R.id.close);
        closeBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mBluetoothAdapter.disable();
                mConnectThread = null;
            }
        });
        startBt = (Button) view.findViewById(R.id.start);
        startBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mBluetoothAdapter != null) {
                    clearDevices();//开始搜索前清空上一次的列表
                    mBluetoothAdapter.startDiscovery();
                    addMessage("开始搜索蓝牙");
                } else {
                    openBT();
                    if (mBluetoothAdapter != null) {
                        clearDevices();//开始搜索前清空上一次的列表
                        mBluetoothAdapter.startDiscovery();
                        addMessage("开始搜索蓝牙");
                    }
                }
            }
        });
        stopBt = (Button) view.findViewById(R.id.stop);
        stopBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mBluetoothAdapter != null && mBluetoothAdapter.isDiscovering()) {
                    mBluetoothAdapter.cancelDiscovery();
                }
            }
        });
        sendBt = (Button) view.findViewById(R.id.send);
        sendBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String msg = inputEt.getText().toString();
                if (TextUtils.isEmpty(msg)) {
                    Toast.makeText(getActivity(), "消息为空", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (mConnectThread != null) {//证明我主动去链接别人了
                    mConnectThread.write(msg);
                } else if (mAcceptThread != null) {
                    mAcceptThread.write(msg);
                }
                addMessage("发送消息：" + msg);
                inputEt.setText("");
            }
        });
        listView_msg = (ListView) view.findViewById(R.id.msglist);
        listView_device = (ListView) view.findViewById(R.id.devices);

        listView_device.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                BluetoothDevice bluetoothDevice = devicelist.get(i);
                mConnectThread = new ConnectThread(bluetoothDevice);
                mConnectThread.start();
            }
        });

    }

    private void openBT() {
        if (mBluetoothAdapter == null) {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        //1.设备不支持蓝牙，结束应用
        if (mBluetoothAdapter == null) {
            addMessage("该设备不支持蓝牙");
            return;
        }
        //2.判断蓝牙是否打开
        if (!mBluetoothAdapter.enable()) {
            //没打开请求打开
            Intent btEnable = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(btEnable, REQUEST_BT_ENABLE_CODE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_BT_ENABLE_CODE) {
            if (resultCode == RESULT_OK) {
                //用户允许打开蓝牙
                addMessage("用户同意打开蓝牙");
            } else if (resultCode == RESULT_CANCELED) {
                //用户取消打开蓝牙
                addMessage("用户拒绝打开蓝牙");
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    class ConnectThread extends Thread {
        private BluetoothDevice mDevice;
        private BluetoothSocket mSocket;
        private InputStream btIs;
        private OutputStream btOs;
        private boolean canRecv;
        private PrintWriter writer;

        public ConnectThread(BluetoothDevice device) {
            mDevice = device;
            canRecv = true;
        }

        @Override
        public void run() {
            if (mDevice != null) {
                try {
                    //获取套接字
                    BluetoothSocket temp = mDevice.createInsecureRfcommSocketToServiceRecord(UUID.fromString(BT_UUID));
                    //mDevice.createRfcommSocketToServiceRecord(UUID.fromString(BT_UUID));//sdk 2.3以下使用
                    mSocket = temp;
                    //发起连接请求
                    if (mSocket != null) {
                        mSocket.connect();
                    }
                    sendHandlerMsg("连接 " + mDevice.getName() + "成功！");
                    //获取输入输出流
                    btIs = mSocket.getInputStream();
                    btOs = mSocket.getOutputStream();

                    //通讯-接收消息
                    BufferedReader reader = new BufferedReader(new InputStreamReader(btIs, "UTF-8"));
                    String content = null;
                    while (canRecv) {
                        content = reader.readLine();
                        sendHandlerMsg("收到消息：" + content);
                        addMessage("收到消息：" + content);
                    }


                } catch (IOException e) {
                    e.printStackTrace();
                    sendHandlerMsg("错误：" + e.getMessage());
                } finally {
                    try {
                        if (mSocket != null) {
                            mSocket.close();
                        }
                        //btIs.close();//两个输出流都依赖socket，关闭socket即可
                        //btOs.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        sendHandlerMsg("错误：" + e.getMessage());
                    }
                }
            }
        }

        private void sendHandlerMsg(String content) {
            Message msg = mHandler.obtainMessage();
            msg.what = 1001;
            msg.obj = content;
            mHandler.sendMessage(msg);
        }

        public void write(String msg) {
            if (btOs != null) {
                try {
                    if (writer == null) {
                        writer = new PrintWriter(new OutputStreamWriter(btOs, "UTF-8"), true);
                    }
                    writer.println(msg);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    writer.close();
                    sendHandlerMsg("错误：" + e.getMessage());
                }
            }
        }
    }

    class AcceptThread extends Thread {
        private BluetoothServerSocket mServerSocket;
        private BluetoothSocket mSocket;
        private InputStream btIs;
        private OutputStream btOs;
        private PrintWriter writer;
        private boolean canAccept;
        private boolean canRecv;

        public AcceptThread() {
            canAccept = true;
            canRecv = true;
        }

        @Override
        public void run() {
            try {
                //获取套接字
                BluetoothServerSocket temp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord("TEST", UUID.fromString(BT_UUID));
                mServerSocket = temp;
                //监听连接请求 -- 作为测试，只允许连接一个设备
                if (mServerSocket != null) {
                    // while (canAccept) {
                    mSocket = mServerSocket.accept();
                    sendHandlerMsg("有客户端连接");
                    // }
                }
                //获取输入输出流
                btIs = mSocket.getInputStream();
                btOs = mSocket.getOutputStream();
                //通讯-接收消息
                BufferedReader reader = new BufferedReader(new InputStreamReader(btIs, "UTF-8"));
                String content = null;
                while (canRecv) {
                    content = reader.readLine();
                    sendHandlerMsg("收到消息：" + content);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (mSocket != null) {
                        mSocket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    sendHandlerMsg("错误：" + e.getMessage());
                }
            }
        }

        private void sendHandlerMsg(String content) {
            Message msg = mHandler.obtainMessage();
            msg.what = 1001;
            msg.obj = content;
            mHandler.sendMessage(msg);
        }

        public void write(String msg) {
            if (btOs != null) {
                try {
                    if (writer == null) {
                        writer = new PrintWriter(new OutputStreamWriter(btOs, "UTF-8"), true);
                    }
                    writer.println(msg);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    writer.close();
                    sendHandlerMsg("错误：" + e.getMessage());
                }
            }
        }
    }

    public void addMessage(String msg){
        msglist.add(msg);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                getActivity(),android.R.layout.simple_list_item_1,msglist
        );
        listView_msg.setAdapter(adapter);
        listView_msg.setSelection(adapter.getCount() - 1);
    }

    public void addDevice(BluetoothDevice device){
        devicelist.add(device);
        devices.add(device.getName());
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                getActivity(),android.R.layout.simple_list_item_1,devices
        );
        listView_device.setAdapter(adapter);
    }

    public void clearDevices(){
        if(devicelist!=null) {
            devicelist.clear();
            devices.clear();
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                getActivity(),android.R.layout.simple_list_item_1,devices
        );
        listView_device.setAdapter(adapter);
    }
}
