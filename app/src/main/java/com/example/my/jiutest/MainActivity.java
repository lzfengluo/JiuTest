package com.example.my.jiutest;

import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.serialport.SerialPortSpd;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.speedata.libutils.DataConversionUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class MainActivity extends AppCompatActivity {

    private TextView stateTv;
    private Button startBtn;

    //    串口路径
    public static final String SERIAL_TTYMT2 = "/dev/ttyMT2";
    //    波特率
    public static final int brd = 9600;
    //    句柄
    private int fd;
    //    串口对象
    private SerialPortSpd mSerialPortSpd;
    //    开始指令
    private byte[] str;
    //    返回结果
    private byte[] returnStr;

    private Handler handler = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        startInit();
//        开启一个线程，start()
        new Thread() {
            @Override
            public void run() {
                super.run();
                while (!isInterrupted()) {
//                    获取句柄
                    fd = mSerialPortSpd.getFd();
                    try {
//                        读串口，获取信息
                        returnStr = mSerialPortSpd.ReadSerial(fd, 1024);
                        if (returnStr != null) {
                            Message msg = new Message();
                            msg.what = 1;
                            msg.obj = returnStr;
                            handler.sendMessage(msg);
                        }
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == 1) {
                    super.handleMessage(msg);
                    returnStr = (byte[]) msg.obj;
                    String s = DataConversionUtils.byteArrayToAscii(returnStr);
                    if ("NOST".equals(s)) {
                        stateTv.setText("状态：没有就绪");
                        stateTv.setTextColor(Color.parseColor("#5c5c5c"));
                    } else if ("ALAM".equals(s)) {
                        stateTv.setText("状态：测酒超时");
                        stateTv.setTextColor(Color.parseColor("#8C8117"));
                    } else if ("0000".compareTo(s) <= 0) {//字符串比较，若"0000"<s，返回值为-1，相等为0 ，大于为1
                        double num = Double.parseDouble(s);//将数字字符串转换成double类型
                        num = num / 10;
                        if ("0200".compareTo(s) > 0) {
                            stateTv.setText("状态：正常\n数值：" + num + "mg/100ml");
//                            Color.parseColor(String)返回值为int类型，设置颜色
                            stateTv.setTextColor(Color.parseColor("#04DF04"));
                        } else if ("0800".compareTo(s) > 0) {
                            stateTv.setText("状态：饮酒\n数值：" + num + "mg/100ml");
                            stateTv.setTextColor(Color.parseColor("#C14747"));
                        } else {
                            stateTv.setText("状态：醉酒\n数值：" + num + "mg/100ml");
                            stateTv.setTextColor(Color.parseColor("#ff0000"));
                        }
                    }
//                    当接收到信息并处理完之后，恢复按钮为可用状态，并恢复显示文字
                    startBtn.setEnabled(true);
                    startBtn.setText("开始检测");
                }
            }
        };
    }

    //    获取控件id
    public void initView() {
        stateTv = (TextView) findViewById(R.id.state_tv);//获取显示文本框
        startBtn = (Button) findViewById(R.id.start_btn);//获取按钮
    }

    public void startInit() {
        mSerialPortSpd = new SerialPortSpd();
//        获取句柄
        fd = mSerialPortSpd.getFd();
//        打开串口
        try {
            mSerialPortSpd.OpenSerial(SERIAL_TTYMT2, brd);
        } catch (IOException e) {
            e.printStackTrace();
        }

        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//              获取句柄
                fd = mSerialPortSpd.getFd();
                str = DataConversionUtils.HexString2Bytes("55");
//              发送数据
                mSerialPortSpd.WriteSerialByte(fd, str);
//                将按钮置为禁用状态，改变显示文字
                startBtn.setEnabled(false);
                startBtn.setText("正在检测");
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        关闭串口
        mSerialPortSpd.CloseSerial(fd);
    }
}
