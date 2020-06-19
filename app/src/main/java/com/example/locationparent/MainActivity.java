package com.example.locationparent;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.Polyline;
import com.amap.api.maps.model.PolylineOptions;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    MapView mapView = null;
    AMap  aMap = null;
    MarkerOptions markerOptions;
    List<LatLng> latLngs;
    double longitude;
    double latitude;
    String time;

    SQLiteDatabase db;
    MyHelper myHelper;
    EditText search_start;
    EditText search_end;
    Button now;
    Button history;
    EditText et_tel;
    ImageView call;
    private  SharedPreferences sp = null;
    private SharedPreferences.Editor editor;

    String[] permissions = new String[]{
            Manifest.permission.INTERNET,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_PHONE_STATE};
    List<String> permissionList = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (setPermission()) {
            init();
            Intent intent = new Intent(this, recvService.class);
            bindService(intent, connection, BIND_AUTO_CREATE);
            IntentFilter filter = new IntentFilter("com.example.broadcast");
            registerReceiver(broadcastReceiver, filter);
            mapView = findViewById(R.id.pmap);
            aMap = mapView.getMap();
            mapView.onCreate(savedInstanceState);
            gettel();
            latLngs = new ArrayList<LatLng>();
        }

    }
    private void init(){
        search_start = findViewById(R.id.search_start);
        search_end = findViewById(R.id.search_end);
        now = findViewById(R.id.now);
        now.setOnClickListener(this);
        history = findViewById(R.id.history);
        history.setOnClickListener(this);
        et_tel = findViewById(R.id.et_tel);
        call = findViewById(R.id.iv_call);
        call.setOnClickListener(this);
        myHelper = new MyHelper(this, "example.db", null,1);
    }
    boolean setPermission(){
        permissionList.clear();
        for(int i=0;i<permissions.length;i++){
            if(ContextCompat.checkSelfPermission(this,permissions[i])!= PackageManager.PERMISSION_GRANTED){
                permissionList.add(permissions[i]);
            }
        }
        if(!permissionList.isEmpty()){
            String[] permission2 =permissionList.toArray(new String[permissionList.size()]);
            ActivityCompat.requestPermissions(this,permission2,1);
            return false;
        }
        return true;

    }
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
                longitude = intent.getExtras().getDouble("longitude");
                latitude = intent.getExtras().getDouble("latitude");
                time = intent.getExtras().getString("time");
                setMarker(longitude, latitude,time);
        }
    };

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.now) {
            db = myHelper.getReadableDatabase();
            getLocationRecent();
        }else if(v.getId() == R.id.history){
            String start = search_start.getText().toString().trim();
            String end = search_end.getText().toString().trim();
            if(start.equals("") || end.equals("")){
                Toast.makeText(this,"没有输入时间",Toast.LENGTH_SHORT).show();
            }else{
                    getLocationHistory(Integer.parseInt(start, 10), Integer.parseInt(end, 10));
            }
        }else if(v.getId() == R.id.iv_call) {
            String tel = et_tel.getText().toString().trim();
            int level = 0;
            for(int i=0;i<tel.length();i++) {
                if (Character.isDigit(tel.charAt(i))) {
                    level++;
                }
            }
            if(level==11){
                callPhone(tel);
            }else{
                Toast.makeText(this,"输入号码不对",Toast.LENGTH_SHORT).show();
            }
        }

    }

    void gettel(){
        sp = getSharedPreferences("data",MODE_PRIVATE);
        String data = sp.getString("et_tel", "");
        et_tel.setText(data);
    }
    void callPhone(String tel){
        sp = getSharedPreferences("data",MODE_PRIVATE);
        editor = sp.edit();
        editor.putString("et_tel", tel);
        editor.commit();
        Intent dialIntent =  new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + tel));//跳转到拨号界面，同时传递电话号码
        startActivity(dialIntent);

    }
    void getLocationRecent(){
        aMap.clear();
        Cursor cursor = db.rawQuery("select * from aaa ORDER BY time desc", null);
        if (cursor.getCount() == 0){
            Toast.makeText(this, "没有数据",Toast.LENGTH_SHORT).show();
            return;
        }
        cursor.moveToFirst();
        longitude = cursor.getDouble(cursor.getColumnIndex("longitude"));
        latitude = cursor.getDouble(cursor.getColumnIndex("latitude"));
        time = cursor.getString(cursor.getColumnIndex("time"));
        setMarker(longitude,latitude,time);
        while (cursor.moveToNext()){
            Log.i("Database", "getLocationRecentTim: "+cursor.getString(cursor.getColumnIndex("longitude")));
            Log.i("Database", "getLocationRecentLat: "+cursor.getDouble(cursor.getColumnIndex("latitude")));
            Log.i("Database", "getLocationRecentLon: "+cursor.getDouble(cursor.getColumnIndex("time")));
        }
        setMarker(longitude, latitude,time);
        cursor.close();
    }
    void getLocationHistory(int start,int end){
        db = myHelper.getReadableDatabase();
        String sql;
        aMap.clear();
        if (start < end) {
            if (start < 10) {
                sql = "select * from aaa where time(time)" +
                        ">=time('0" + start + ":00:00') and" +
                        " time(time)<time('" + end + ":00:00')";
            }else {
                sql = "select * from aaa where time(time)" +
                        ">=time('" + start + ":00:00') and" +
                        " time(time)<time('" + end + ":00:00')";
            }
            Cursor cursor = db.rawQuery(sql, null);
            if (cursor.getCount() == 0){
                Log.i("Database", "NoData");
                cursor.close();
                return;
            }
            cursor.moveToFirst();
            latitude = cursor.getDouble(cursor.getColumnIndex("latitude"));
            longitude = cursor.getDouble(cursor.getColumnIndex("longitude"));
            time = cursor.getString(cursor.getColumnIndex("time"));
            setMarker(longitude,latitude,time);

            while (cursor.moveToNext()){
                latitude = cursor.getDouble(cursor.getColumnIndex("latitude"));
                longitude = cursor.getDouble(cursor.getColumnIndex("longitude"));
                time = cursor.getString(cursor.getColumnIndex("time"));
                latLngs.add((new LatLng(latitude,longitude)));
                setMarker(longitude, latitude,time);
            }
            aMap.addPolyline(new PolylineOptions().
                    addAll(latLngs).width(10).color(Color.argb(127, 0, 0, 255)));
            cursor.close();
        }else {
            Toast.makeText(this, "时间输入不对!", Toast.LENGTH_SHORT).show();
        }
    }
    void setMarker(double longitude, double latitude,String time){
        aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 16));
        markerOptions = new MarkerOptions();
        markerOptions.position(new LatLng(latitude, longitude));
        markerOptions.title(time);
        markerOptions.visible(true);
        aMap.addMarker(markerOptions);

    }
    @Override
    protected void onDestroy() {//销毁地图
        super.onDestroy();
        mapView.onDestroy();
        unbindService(connection);
    }

    @Override
    protected void onResume() {//重新绘制地图
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {//暂停地图的绘制
        super.onPause();
        mapView.onPause();
    }

}
