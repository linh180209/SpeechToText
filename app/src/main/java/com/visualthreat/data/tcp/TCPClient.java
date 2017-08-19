package com.visualthreat.data.tcp;

import android.nfc.Tag;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Arrays;

/**
 * Created by USER on 2/1/2017.
 */

public class TCPClient {
    private static String TAG = "TCPClient";
    public static String    READ            = "read";
    private static String   ADDRESS         = "10.10.100.254";
    //private static String   ADDRESS         = "192.168.100.20";
    private static int      PORT            = 8899;
    //private static int      PORT            = 8000;
    private Socket mSocket;
    private TCPClientReadHandler handler;
    private Thread mThreadReceiveData;
    public TCPClient(TCPClientReadHandler handler) {
        this.handler = handler;
    }

    /**
     * Init socket tcp/ip
     */
    public void InitSocket() throws IOException {
        Log.d(TAG,"InitSocket");
        SocketAddress address = new InetSocketAddress(ADDRESS,PORT);
        mSocket = new Socket();
        mSocket.connect(address);
        ReceiveMessage();
    }

    /**
     * Destroy TCP/IP
     */
    public void DestroySocket(){
        try {
            if (mSocket != null) {
                mSocket.close();
                mSocket = null;
            }

            if(mThreadReceiveData != null) {
                if(mThreadReceiveData.isAlive()) {
                    mThreadReceiveData.interrupt();
                    mThreadReceiveData = null;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * send message
     * @param data data
     */
    public void SendMessage(String data) {
        OutputStream mOutputStream = GetOutputStream(mSocket);
        byte[] bytesSend = new byte[2048];
        if(mOutputStream != null) {
            try {
                data = data + "\r\n";
                bytesSend = data.getBytes("UTF-8");
                if (mSocket != null) {
                    mOutputStream.write(bytesSend, 0, bytesSend.length);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Thread receive message tcp/ip
     */
    int totalPackage = 0;
    private void ReceiveMessage() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                BufferedReader mBufferIn = null ;
                try {
                if(mSocket != null){
                    mBufferIn = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
                }
                while (mSocket != null) {
                        if(mBufferIn != null) {
                            try {
                                String mServerMessage = mBufferIn.readLine();
                                totalPackage ++;
                                Log.d(TAG,"mServerMessage :"+totalPackage+"-"+ mServerMessage);
                                if(mServerMessage != null) {
                                    handler.onEvent(mServerMessage);
                                }
                                // the socket tcp/ip server will be crashed when send about 55000 packet
                                // the app will try to connect again to continue receive data
                                // this problem is server's. If the server tcp/ip be to fixed, please remove this code
                                else {
                                    InitSocket();
                                    break;
                                }
                            }
                            catch (Exception e){
                                Log.d(TAG,"Exception: " + e);
                            }
                        }
                    }
                }catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                    Log.e(TAG, "TCP Reader Exiting...");
                }
            }
        };
        mThreadReceiveData = new Thread(runnable);
        mThreadReceiveData.start();
    }

    /**
     * Get inputStreaming
     * @param socket Socket
     * @return inputStream
     */
    private InputStream GetInputStream(Socket socket){
        if(socket == null)
            return null;
        InputStream inputStream = null;
        try {
            inputStream = socket.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return inputStream;
    }

    /**
     * Get outputStream
     * @param socket Socket
     * @return outputStream
     */
    private OutputStream GetOutputStream(Socket socket) {
        if(socket == null)
            return null;
        OutputStream outputStream = null;
        try {
            outputStream = socket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outputStream;
    }
}
