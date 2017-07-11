package com.example.saber.aidlbindertest.mulprocess;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.example.saber.aidlbindertest.Messenger.MessengerActivity;
import com.example.saber.aidlbindertest.R;
import com.example.saber.aidlbindertest.aidl.BookManagerActivity;
import com.example.saber.aidlbindertest.binderpool.BInderPoolActivity;
import com.example.saber.aidlbindertest.provider.ProviderActivity;
import com.example.saber.aidlbindertest.socket.TCPClientActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();

        //文件存储
        Button button = (Button) findViewById(R.id.btn_start_secondActivity);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,SecondActivity.class);
                startActivity(intent);
            }
        });

        //启动MessengerActivity
        Button button1 = (Button) findViewById(R.id.btn_start_MessengerActivity);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,MessengerActivity.class);
                startActivity(intent);
            }
        });


        //启动BookManagerActivity
        Button button2 = (Button) findViewById(R.id.btn_start_BookManagerActivity);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,BookManagerActivity.class);
                startActivity(intent);
            }
        });

        //启动ProviderActivity
        Button button3 = (Button) findViewById(R.id.btn_start_ProviderActivity);
        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,ProviderActivity.class);
                startActivity(intent);
            }
        });

        //启动TCPClientActivity
        Button button4 = (Button) findViewById(R.id.btn_start_TCPClientActivity);
        button4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,TCPClientActivity.class);
                startActivity(intent);
            }
        });


        //启动BinderPoolActivity
        Button button5 = (Button) findViewById(R.id.btn_start_BinderPoolActivity);
        button5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,BInderPoolActivity.class);
                startActivity(intent);
            }
        });


        //运行时权限
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
        }else {
            //使用文件共享数据，序列化，写入文件
            persistToFile();
        }


    }

    /**
     * 序列化对象，并写入文件
     */
    private void persistToFile() {
        new Thread(new Runnable() {
            @Override
            public void run() {

                User user = new User(1,"hello world",false);
                File dir = new File(MyConstants.CHAPTER_2_PATH);
                if(!dir.exists()){
                    dir.mkdirs();
                }

                File cachedFile = new File(MyConstants.CACHE_FILE_PATH);
                try {
                    cachedFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ObjectOutputStream objectOutputStream = null;

                try {
                    if(cachedFile.exists()){
                        objectOutputStream = new ObjectOutputStream(new FileOutputStream(cachedFile));
                        objectOutputStream.writeObject(user);
                        Log.d(TAG,"persist user:"+user);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }finally {
                    if (objectOutputStream != null ){
                        try {
                            objectOutputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();
    }
}
