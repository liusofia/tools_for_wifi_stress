package com.example.wifistress.wifistresstest;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private static final int MAX_COUNT = 20;
    private static final String TAG = "xiaoxi";
    private WifiManager mWifiManager;
    private Timer mTimer;
    public boolean isRunning = false;
    public int mMaxTestCount = MAX_COUNT;
    public int mCurrentCount = 0;
    public int mPassCount = 0;
    private PowerManager.WakeLock mWakeLock = null;

    private String mPingIpAddrResult = null;
    private String mIpAddress = "8.8.8.8";

    private TextView maxCount;
    private TextView currentCount;
    private TextView passCount;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseWakeLock();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    private void pingIpAddr() {
        try {
            Process p = Runtime.getRuntime().exec("ping -c 1 -w 7 " + mIpAddress);
            int status = p.waitFor();
            if (status == 0) {
                Log.d(TAG,"status == 0");
                mPingIpAddrResult = "Pass";
                mPassCount++;
            } else {
                mPingIpAddrResult = "Fail: IP addr not reachable";
            }
        } catch (IOException e) {
            mPingIpAddrResult = "Fail: IOException";
        } catch (InterruptedException e) {
            mPingIpAddrResult = "Fail: InterruptedException";
        }
    }

    private void init() {
        mWifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        acquireWakeLock();
        maxCount = (TextView) findViewById(R.id.max_count);
        currentCount = (TextView) findViewById(R.id.current_count);
        passCount = (TextView) findViewById(R.id.pass_count);
        maxCount.setText(getString(R.string.max_count) + mMaxTestCount);
        passCount.setText(getString(R.string.pass_count) + mPassCount);
        currentCount.setText(getString(R.string.current_count) + mCurrentCount);
        mWifiManager.setWifiEnabled(true);
    }

    //获取电源锁，保持该服务在屏幕熄灭时仍然获取CPU时，保持运行
    private void acquireWakeLock() {
        if (null == mWakeLock) {
            PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "PostLocationService");
            if (null != mWakeLock) {
                mWakeLock.acquire();
            }
        }
    }
    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case 0:
                    isRunning = false;
                    if (mTimer != null)
                        mTimer.cancel();
                    break;
                case 1:
                    currentCount.setText(getString(R.string.current_count) + mCurrentCount);
                    passCount.setText(getString(R.string.pass_count) + mPassCount);
                    Log.d(TAG, "already test time:" + mCurrentCount);

                    break;

                default:
                    break;
            }
        }
    };

    public void stopTest(View view) {
        isRunning = false;
        if (mTimer != null)
            mTimer.cancel();
    }

    public int incCurCount() {
        mCurrentCount = mCurrentCount + 1;
        return mCurrentCount;
    }

    //释放设备电源锁
    private void releaseWakeLock() {
        if (null != mWakeLock) {
            mWakeLock.release();
            mWakeLock = null;
        }
    }

    public void startTest(View view) {
        isRunning = true;
        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!isRunning || (mMaxTestCount != 0 && mCurrentCount >= mMaxTestCount)) {
                    mHandler.sendEmptyMessage(0);
                    mTimer.cancel();
                    return;
                } else {
                    if (mWifiManager.getWifiState() == mWifiManager.WIFI_STATE_DISABLED) {
                        mWifiManager.setWifiEnabled(true);
                        Log.d(TAG, "*********wifistate is closed, try open wifi now!*****");
                    } else if (mWifiManager.getWifiState() == mWifiManager.WIFI_STATE_ENABLED) {
                        pingIpAddr();
                        if (null != mPingIpAddrResult) {
                            Log.d(TAG, mPingIpAddrResult);
                        }
                        mWifiManager.setWifiEnabled(false);
                        Log.d(TAG, "*********wifistate is opened, try close wifi now!******");
                        incCurCount();
                    }
                    mHandler.sendEmptyMessage(1);
                }

            }
        }, 2000, 10000);
    }

}
