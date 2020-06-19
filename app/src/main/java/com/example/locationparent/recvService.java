package com.example.locationparent;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

import static android.content.ContentValues.TAG;

public class recvService extends Service {
    final int port = 8000;
    Socket connection;
    ServerSocket serverSocket;
    InputStreamReader inputStreamReader;
    InputStream inputStream;

    private MyHelper myHelper;

    double longitude = 0;
    double latitude = 0;
    String time;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        connectServer();
        myHelper = new MyHelper(this, "example.db", null, 1);
        myHelper.getWritableDatabase();
        return null;
    }

    public void connectServer(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    serverSocket = new ServerSocket(port);
                    connection = serverSocket.accept();
                    Log.i(TAG, "run: Connected");
                    inputStream = connection.getInputStream();
                    inputStreamReader = new InputStreamReader(inputStream);
                    getData(inputStreamReader);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void getData(InputStreamReader inputStreamReader){
        try {
            BufferedReader br = new BufferedReader(inputStreamReader);
            String data;
            SQLiteDatabase db = myHelper.getWritableDatabase();
            ContentValues values = new ContentValues();

            while ((data = br.readLine()) != null) {
                Log.i(TAG,"ssss"+data);
                if (data.substring(0, data.indexOf(":")).equals("longitude")){
                    String[] str = data.split(":");
                    longitude = Double.parseDouble(str[1]);
                    values.put("longitude", longitude);
                    Log.i(TAG, "longitude:"+longitude);
                }else if (data.substring(0, data.indexOf(":")).equals("latitude")){
                    String[] str = data.split(":");
                    latitude = Double.parseDouble(str[1]);
                    values.put("latitude", latitude);
                    Log.i(TAG, "latitude:"+latitude);
                }else if (data.substring(0, data.indexOf(":")).equals("time")){
                    String str = data.substring(data.indexOf(":"));
                    String[] strResult = str.split(" ");
                    values.put("time", strResult[3]);
                    Log.i(TAG, "time:"+strResult[3]);
                    db.insert("aaa", null, values);
                    sendData(longitude, latitude,time);
                    Log.i(TAG,"longitude:"+longitude+"latitude:"+latitude);
                    values.clear();
                }
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private void sendData(final double longitude, final double latitude,final String time){
        new Thread(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent("com.location.broadcast");
                intent.putExtra("longitude", longitude);
                intent.putExtra("latitude", latitude);
                intent.putExtra("time",time);
                sendBroadcast(intent);
            }
        }).start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disSocket();
    }

    private void disSocket(){
        //如果不为空，则断开socket
        if(connection !=null){
            try {
                inputStreamReader.close();
                connection.close();
                connection = null;
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        if(serverSocket !=null){
            try {
                serverSocket.close();
                serverSocket = null;
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
