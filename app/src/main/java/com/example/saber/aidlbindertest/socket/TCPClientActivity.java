package com.example.saber.aidlbindertest.socket;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.saber.aidlbindertest.R;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TCPClientActivity extends AppCompatActivity implements View.OnClickListener{

    private static final String TAG = "TCPClientActivity";

    private static final int MESSAGE_RECEIVE_NEW_MSG = 1;
    private static final int MESSAGE_SOCKET_CONNECTED = 2;

    private Button mSendButton;
    private TextView mMessageTextView;
    private EditText mMessageEditText;

    private PrintWriter mPrintWriter;
    private Socket mClientSocket;

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case MESSAGE_RECEIVE_NEW_MSG:
                    mMessageTextView.setText(mMessageTextView.getText() + (String)msg.obj);
                    break;
                case MESSAGE_SOCKET_CONNECTED:
                    mSendButton.setEnabled(true);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tcpclient);

        mMessageTextView = (TextView) findViewById(R.id.msg_container);
        mSendButton = (Button) findViewById(R.id.send);
        mSendButton.setOnClickListener(this);
        mMessageEditText = (EditText) findViewById(R.id.msg);

        Intent service = new Intent(this,TCPServerService.class);
        startService(service);

        new Thread(){
            @Override
            public void run() {
                //连接服务端
                connectTCPServer();
            }
        }.start();

    }

    //通过Socket连接服务端
    private void connectTCPServer() {

        Socket socket = null;
        //采用超时重连策略连接服务端
        while(socket == null){
            try {
                socket = new Socket("192.168.1.130",8688);
                mClientSocket = socket;

                //发送到客户端 , 设置true参数就不需要手动的刷新输出流
                mPrintWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())),true);

                mHandler.sendEmptyMessage(MESSAGE_SOCKET_CONNECTED);
                Log.d(TAG,"connect server success");

            } catch (IOException e) {
                SystemClock.sleep(1000);
                Log.e(TAG,"connect tcp server failed,retry...");
            }
        }

        try {
            //接收服务端的消息
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            while(!TCPClientActivity.this.isFinishing()){
                String msg = br.readLine();
                Log.d(TAG,"receive:"+msg);
                if(msg != null){
                    String time = formatDateTime(System.currentTimeMillis());
                    final String showedMsg = "server "+ time + ":"+ msg+"\n";
                    mHandler.obtainMessage(MESSAGE_RECEIVE_NEW_MSG,showedMsg).sendToTarget();
                }
            }

            Log.e(TAG,"client is quiting...");
            mPrintWriter.close();
            br.close();
            socket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private String formatDateTime(long l) {
        return new SimpleDateFormat("HH:mm:ss").format(new Date(l));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            //客户端发送消息到服务端
            case R.id.send:
                final String msg = mMessageEditText.getText().toString();
                if(!TextUtils.isEmpty(msg) && mPrintWriter != null){
                    mPrintWriter.println(msg);
                    mMessageEditText.setText("");
                    String time = formatDateTime(System.currentTimeMillis());
                    final String showedMsg = "self "+ time + ":"+msg+"\n";
                    mMessageTextView.setText(mMessageTextView.getText() + showedMsg);
                }

                break;
        }
    }

    @Override
    protected void onDestroy() {
        if(mClientSocket != null){
            try {
                mClientSocket.shutdownInput();
                mClientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        super.onDestroy();
    }
}
