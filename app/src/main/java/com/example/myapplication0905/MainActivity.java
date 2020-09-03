package com.example.myapplication0905;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.EditText;

import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.Constants;
import android.text.TextUtils;
import android.os.Environment;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.agora.rtm.ErrorInfo;
import io.agora.rtm.RtmChannel;
import io.agora.rtm.RtmChannelAttribute;
import io.agora.rtm.RtmChannelListener;
import io.agora.rtm.RtmChannelMember;
import io.agora.rtm.RtmClient;
import io.agora.rtm.RtmClientListener;
import io.agora.rtm.RtmMessage;

import java.util.Map;
import java.util.Random;
import io.agora.rtm.ResultCallback;

import android.widget.Button;

import android.widget.Spinner;

import static io.agora.rtm.RtmStatusCode.ConnectionChangeReason.CONNECTION_CHANGE_REASON_LOGIN_SUCCESS;

public class MainActivity  extends AppCompatActivity {

    private String channel;
    private String appId;

    //RTC
    private RtcEngine mRtcEngine;
    private int mUid; //RTCのuid保持用

    //RTM
    private RtmClient mRtmClient;
    private RtmChannel mRtmChannel;
    private List<RtmClientListener> mListenerList = new ArrayList<>();

    //muteボタン制御フラグ
    private boolean mAudioMuted1 = false;
    private boolean mAudioMuted2 = true;
    private boolean mAudioMuted3 = true;

    //hostのuid
    private static final int HOST_UID = 100000;
    //audienceの最大uid
    private static final int AUDIENCE_MAX_UID = 99999;

    //ログ用
    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    //パーミッション用
    private static final int BASE_VALUE_PERMISSION = 0X0001;
    private static final int PERMISSION_REQ_ID_RECORD_AUDIO = BASE_VALUE_PERMISSION + 1;
    private static final int PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE = BASE_VALUE_PERMISSION + 3;

    private static final String COLON = ":";

    public boolean checkSelfPermission(String permission, int requestCode) {
        Log.i(LOG_TAG, "checkSelfPermission " + permission + " " + requestCode);
        if (ContextCompat.checkSelfPermission(this,
                permission)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{permission},
                    requestCode);
            return false;
        }
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(LOG_TAG,"onCreate()");
        super.onCreate(savedInstanceState);

        //実行時のパーミッション リクエスト
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO, PERMISSION_REQ_ID_RECORD_AUDIO) &&
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE)) {
            Log.i(LOG_TAG,"checkSelfPermissionResult:AllTrue");
        }

        //画面表示
        setContentView(R.layout.activity_main);
    }


    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {

        @Override
        public void onJoinChannelSuccess(String channel,
                                         final int uid,
                                         int elapsed) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.i(LOG_TAG, "onJoinChannelSuccess() " + uid);
                    //画面の制御
                    dispEnable();
                    //uid設定
                    dispUid(uid);
                    //入室時ミュート
                    if(uid==HOST_UID){
                        doVoiceLocalMute(false);
                    }else{
                        doVoiceLocalMute(true);
                    }
                    //Status
                    TextView tvStatus = (TextView) findViewById(R.id.join_status);
                    tvStatus.setText("Join Success");
                }
            });
        }

        @Override
        public void onUserJoined(final int uid, int elapsed) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.i(LOG_TAG, "onUserJoined() " + uid);
                    //画面制御
                    dispUid(uid);
                }
            });
        }

        @Override
        public void onLeaveChannel(RtcStats stats) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.i(LOG_TAG, "onLeaveChannel()");
                }
            });
        }

        @Override
        public void onUserOffline(int 	uid, int 	reason){
            if(uid==HOST_UID){
                TextView tx1 = (TextView) findViewById(R.id.textView1);
                tx1.setText(getString(R.string.label_uid));
                TextView tx1Req = (TextView) findViewById(R.id.textReq1);
                tx1Req.setText(getString(R.string.label_null));

            }else{
                TextView tx2 = (TextView) findViewById(R.id.textView2);
                tx2.setText(getString(R.string.label_uid));
                TextView tx3 = (TextView) findViewById(R.id.textReq2);
                tx3.setText(getString(R.string.label_null));
                TextView tx4 = (TextView) findViewById(R.id.textView3);
                tx2.setText(getString(R.string.label_uid));
                TextView tx5 = (TextView) findViewById(R.id.textReq3);
                tx3.setText(getString(R.string.label_null));
            }
        }

        @Override
        public void onUserMuteAudio(final int uid, final boolean muted) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.i(LOG_TAG, "onUserMuteAudio(uid " + uid + " muted " + muted);
                    dispReq(uid,muted);
                }
            });
        }

    };


    private void dispUid(int uid) {
        TextView tv1 = (TextView) findViewById(R.id.textView1);
        TextView tv2 = (TextView) findViewById(R.id.textView2);
        TextView tv3 = (TextView) findViewById(R.id.textView3);
        if(uid==HOST_UID){
            tv1.setText(String.valueOf(uid));
        }else{
            if (tv2.getText().toString().equals(getString(R.string.label_uid))){
                tv2.setText(String.valueOf(uid));
            }else{
                tv3.setText(String.valueOf(uid));
            }
        }
    }


    private void dispReq(int uid, boolean muted) {
        TextView tv2 = (TextView) findViewById(R.id.textView2);
        TextView tv3 = (TextView) findViewById(R.id.textView3);
        if (String.valueOf(uid).equals(tv2.getText().toString())) {
            TextView txReq2 = (TextView) findViewById(R.id.textReq2);
            String msg2 = (muted == true ? getString(R.string.msg_muted) : getString(R.string.msg_unmuted));
            txReq2.setText(msg2);
        }else if (String.valueOf(uid).equals(tv3.getText().toString())) {
            TextView txReq3 = (TextView) findViewById(R.id.textReq3);
            String msg3 = (muted == true ? getString(R.string.msg_muted) : getString(R.string.msg_unmuted));
            txReq3.setText(msg3);
        }
    }

    public void onCheckHost(View view) {
        Log.i(LOG_TAG, "onCheckHost()" );
        dispEnable();
    }

    public void dispEnable(){
        TextView tv = (TextView) findViewById(R.id.login_name);
        Button bt1 = (Button) findViewById(R.id.button1);
        Button bt1unmute = (Button) findViewById(R.id.button1unmute);
        Button bt2 = (Button) findViewById(R.id.button2);
        Button bt2unmute = (Button) findViewById(R.id.button2unmute);
        Button bt3 = (Button) findViewById(R.id.button3);
        Button bt3unmute = (Button) findViewById(R.id.button3unmute);
        Button bt4 = (Button) findViewById(R.id.buttonMute);
        Button bt5 = (Button) findViewById(R.id.buttonUnMute);
        CheckBox ch = (CheckBox) findViewById(R.id.host);
        if (true == ch.isChecked()){
            tv.setText(getString(R.string.label_host));
            bt1.setEnabled(true);
            bt1unmute.setEnabled(true);
            bt2.setEnabled(true);
            bt2unmute.setEnabled(true);
            bt3.setEnabled(true);
            bt3unmute.setEnabled(true);
            bt4.setEnabled(false);
            bt5.setEnabled(false);
        }else{
            tv.setText(getString(R.string.label_audience));
            bt1.setEnabled(false);
            bt1unmute.setEnabled(false);
            bt2.setEnabled(false);
            bt2unmute.setEnabled(false);
            bt3.setEnabled(false);
            bt3unmute.setEnabled(false);
            bt4.setEnabled(true);
            bt5.setEnabled(true);
        }

    }


    public void onClickLogin(View view) {
        Log.i(LOG_TAG, "onClickLogin() ");
        doLogin();
    }
    public void onClickLogout(View view) {
        Log.i(LOG_TAG, "onClickLogout() ");
        doLogout();
    }
    public void onClickJoin(View view) {
        Log.i(LOG_TAG, "onClickJoin() ");
        joinChannel();
    }
    public void onClickLeave(View view) {
        Log.i(LOG_TAG, "onClickLeave() ");
        leaveChannel();
    }
    public void onClickSendMsgMute(View v) {
        Log.i(LOG_TAG, "onClickSendMute() ");
        sendChannelMessage(getString(R.string.msg_mute) + COLON + String.valueOf(mUid));
    }

    public void onClickSendMsgUnMute(View v) {
        Log.i(LOG_TAG, "onClickSendUnMute() ");
        sendChannelMessage(getString(R.string.msg_unmute) + COLON + String.valueOf(mUid));
    }

    public void onVoiceMuteClicked1(View view) {
        Log.i(LOG_TAG, "onVoiceMuteClicked1() ");
        doVoiceLocalMute(true);
        TextView tx1 = (TextView) findViewById(R.id.textReq1);
        tx1.setText(getString(R.string.msg_muted));
    }

    public void onVoiceUnMuteClicked1(View view) {
        Log.i(LOG_TAG, "onVoiceUnMuteClicked1() ");
        doVoiceLocalMute(false);
        TextView tx1 = (TextView) findViewById(R.id.textReq1);
        tx1.setText(getString(R.string.msg_unmuted));
    }

    public void onVoiceMuteClicked2(View view) {
        Log.i(LOG_TAG, "onVoiceMuteClicked2() ");
        TextView tx2 = (TextView) findViewById(R.id.textView2);
        sendChannelMessage(getString(R.string.msg_can_mute) + COLON + tx2.getText().toString());
    }

    public void onVoiceUnMuteClicked2(View view) {
        Log.i(LOG_TAG, "onVoiceUnMuteClicked2() ");
        TextView tx2 = (TextView) findViewById(R.id.textView2);
        sendChannelMessage(getString(R.string.msg_can_unmute) + COLON + tx2.getText().toString());
    }


    public void onVoiceMuteClicked3(View view) {
        Log.i(LOG_TAG, "onVoiceMuteClicked3() ");
        TextView tx3 = (TextView) findViewById(R.id.textView3);
        sendChannelMessage(getString(R.string.msg_can_mute) + COLON + tx3.getText().toString());
    }

    public void onVoiceUnMuteClicked3(View view) {
        Log.i(LOG_TAG, "onVoiceUnMuteClicked3() ");
        TextView tx3 = (TextView) findViewById(R.id.textView3);
        sendChannelMessage(getString(R.string.msg_can_unmute) + COLON + tx3.getText().toString());
    }
        /**
         * API CALL: rtc muteLocalAudioStream
         */
    private void doVoiceLocalMute(boolean flg){
         Log.i(LOG_TAG, "doVoiceLocalMute " + flg);
        mRtcEngine.muteLocalAudioStream(flg);
    }

    /**
     * API CALL:
     *     rtm
     *       createInstance
     *       login
     */
    private void doLogin() {
        Log.i(LOG_TAG, "doLogin()");
        TextView tv = (TextView) findViewById(R.id.login_name);
        String login_name = tv.getText().toString();

        mUid=getUid();
        Log.i(LOG_TAG, "mUid " + mUid);

        //RTM
        try {
            mRtmClient = RtmClient.createInstance(getBaseContext(),
                    getString(R.string.agora_app_id), new RtmClientListener() {
                @Override
                public void onConnectionStateChanged(final int state, final int reason) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.i(LOG_TAG, "onConnectionStateChanged state " + state + " reason " + reason);
                            if (reason == CONNECTION_CHANGE_REASON_LOGIN_SUCCESS) {
                                TextView tvStatus = (TextView) findViewById(R.id.login_status);
                                tvStatus.setText("Login Success");
                            }
                        }
                    });
                }

                @Override
                public void onMessageReceived(RtmMessage rtmMessage, String peerId) {
                    Log.i(LOG_TAG, "onMessageReceived RtmMessage " + rtmMessage + " peerId " + peerId);
                }

                @Override
                public void onTokenExpired() { }

                        @Override
                        public void onPeersOnlineStatusChanged(Map<String, Integer> map) {

                        }
                    });
        } catch (Exception e) {
            Log.e(LOG_TAG, Log.getStackTraceString(e));
            throw new RuntimeException("NEED TO check rtm sdk init fatal error\n" + Log.getStackTraceString(e));
        }

        mRtmClient.login(null, login_name + mUid, new ResultCallback<Void>() {
            @Override
            public void onSuccess(Void responseInfo) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.i(LOG_TAG, "login success");
                        TextView tvStatus = (TextView) findViewById(R.id.login_status);
                        tvStatus.setText("Login Success");
                    }
                });
            }
            @Override
            public void onFailure(ErrorInfo errorInfo) {
                Log.i(LOG_TAG, "login failed: " + errorInfo.getErrorCode());
            }

        });

    }

    public int getUid(){
        Log.i(LOG_TAG, "getUid() ");
        Random random = new Random();
        int rtn = (int)(Math.random()*AUDIENCE_MAX_UID);
        Log.i(LOG_TAG, "uid " + rtn);
        return rtn;
    }



    /**
     * API CALL:
     *     rtc
     *        create
     *        setChannelProfile
     *        setAudioProfile
     *        joinChannel
     *     rtm
     *        createChannel
     *        join
     */
    private void joinChannel() {
        Log.i(LOG_TAG, "joinChannel()");
        appId = getString(R.string.agora_app_id);
        TextView v_channel = (TextView) findViewById(R.id.channel_name);
        channel = v_channel.getText().toString();

        mRtmChannel = mRtmClient.createChannel(channel, new MyChannelListener());

        mRtmChannel.join(new ResultCallback<Void>() {
            @Override
            public void onSuccess(Void responseInfo) {
                Log.i(LOG_TAG, "join channel success");

            }

            @Override
            public void onFailure(ErrorInfo errorInfo) {
                Log.e(LOG_TAG, "join channel failed");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                    }
                });
            }
        });

        if (TextUtils.isEmpty(appId)) {
            throw new RuntimeException("NEED TO use your App ID, get your own ID at https://dashboard.agora.io/");
        }
        try {
            mRtcEngine = RtcEngine.create(getBaseContext(), appId, mRtcEventHandler);
        } catch (Exception e) {
        //    throw new RuntimeException("NEED TO check rtc sdk init fatal error\n" + LOG_TAG.getStackTraceString(e));
        }
        mRtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION);
        //mRtcEngine.enableAudioVolumeIndication(200, 3); // 200 ms

        // Audio profile
        Spinner spProfile = (Spinner)findViewById(R.id.profile_name);
        int idxProfile = spProfile.getSelectedItemPosition();
        // Scenario profile
        Spinner spScenario  = (Spinner)findViewById(R.id.scenario_name);
        int idxScenario = spProfile.getSelectedItemPosition();
        mRtcEngine.setAudioProfile(idxProfile,idxScenario);



        TextView tv = (TextView) findViewById(R.id.login_name);
        if (tv.getText().toString().equals(getString(R.string.label_host))) {
            mRtcEngine.joinChannel(null, channel, null, HOST_UID);
        }else{
            mRtcEngine.joinChannel(null, channel, null, mUid);
        }
        Log.i(LOG_TAG, "join " + channel);



    }

    /**
     * API CALL:
     *     rtc leaveChannel
     *     rtm leave / release / logout
     */
    public final void leaveChannel() {
        Log.i(LOG_TAG, "leaveChannel()");
        //RTC
        if (mRtcEngine != null) {
            mRtcEngine.leaveChannel();
            Log.i(LOG_TAG, "leave " + channel);
        }
        //RTM
        if (mRtmChannel != null) {
            mRtmChannel.leave(null);
            mRtmChannel.release();
            mRtmChannel = null;
        }
        mRtmClient.logout(null);

        TextView tvStatus = (TextView) findViewById(R.id.login_status);
        tvStatus.setText(getString(R.string.label_null));
        TextView tvJoinStatus = (TextView) findViewById(R.id.join_status);
        tvJoinStatus.setText(getString(R.string.label_null));

        TextView tx1 = (TextView) findViewById(R.id.textView1);
        tx1.setText(getString(R.string.label_uid));
        TextView tx1Req = (TextView) findViewById(R.id.textReq1);
        tx1Req.setText(getString(R.string.label_null));
        TextView tx2 = (TextView) findViewById(R.id.textView2);
        tx2.setText(getString(R.string.label_uid));
        TextView tx3 = (TextView) findViewById(R.id.textReq2);
        tx3.setText(getString(R.string.label_null));
        TextView tx4 = (TextView) findViewById(R.id.textView3);
        tx4.setText(getString(R.string.label_uid));
        TextView tx5 = (TextView) findViewById(R.id.textReq3);
        tx5.setText(getString(R.string.label_null));

    }

    /**
     * API CALL:
     *     rtc destroy
     */
    @Override
    protected void onDestroy() {
        Log.i(LOG_TAG, "onDestroy()");
        super.onDestroy();

        //leaveChannel();
        mRtcEngine.destroy();
        mRtcEngine = null;
    }

    /**
     * API CALL: logout from RTM server
     */
    private void doLogout() {
        Log.i(LOG_TAG, "doLogout()");
        mRtmClient.logout(null);
        TextView tvStatus = (TextView) findViewById(R.id.login_status);
        tvStatus.setText(getString(R.string.label_null));
    }

    /**
     * API CALLBACK: rtm channel event listener
     */
    class MyChannelListener implements RtmChannelListener {


        @Override
        public void onMemberCountUpdated(int i) {

        }

        @Override
        public void onAttributesUpdated(List<RtmChannelAttribute> list) {

        }

        @Override
        public void onMessageReceived(final RtmMessage message, final RtmChannelMember fromMember) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String account = fromMember.getUserId();
                    String msg = message.getText();
                    Log.i(LOG_TAG, "onMessageReceived account = " + account + " msg = " + msg);

                    String[] arrMsg = msg.split(":");

                    TextView txReq;
                    Button btMute;
                    //MUTE//
                    if (getString(R.string.msg_mute).equals(arrMsg[0])) {
                        TextView tx2 = (TextView) findViewById(R.id.textView2);
                        if (tx2.getText().toString().equals(arrMsg[1])){
                            Log.i(LOG_TAG, "muteReq uid2 = " + arrMsg[1]);
                            txReq =  (TextView) findViewById(R.id.textReq2);
                            txReq.setText(getString(R.string.msg_mute));
                        }else{
                            Log.i(LOG_TAG, "muteReq uid3 = " + arrMsg[1]);
                            txReq =  (TextView) findViewById(R.id.textReq3);
                            txReq.setText(getString(R.string.msg_unmute));
                        }
                    }

                    //UnMUTE//
                    if (getString(R.string.msg_unmute).equals(arrMsg[0])) {
                        TextView tx2 = (TextView) findViewById(R.id.textView2);
                        if (tx2.getText().toString().equals(arrMsg[1])){
                            Log.i(LOG_TAG, "unmuteReq uid2 = " + arrMsg[1]);
                            txReq =  (TextView) findViewById(R.id.textReq2);
                            txReq.setText(getString(R.string.msg_unmute));
                        }else{
                            Log.i(LOG_TAG, "unmuteReq uid3 = " + arrMsg[1]);
                            txReq =  (TextView) findViewById(R.id.textReq3);
                            txReq.setText(getString(R.string.msg_unmute));
                        }

                    }

                    //You can Mute
                    if (getString(R.string.msg_can_mute).equals(arrMsg[0])) {
                        TextView tx2 = (TextView) findViewById(R.id.textView2);
                        if (mUid==Integer.parseInt(arrMsg[1])){
                            doVoiceLocalMute(true);
                            sendChannelMessage(getString(R.string.msg_muted) + COLON + mUid);
                            if (String.valueOf(mUid).equals(tx2.getText().toString())){
                                Log.i(LOG_TAG, "Mute:OK uid2 = " + arrMsg[1]);
                                txReq = (TextView) findViewById(R.id.textReq2);
                                txReq.setText(getString(R.string.msg_muted));
                            }else{
                                Log.i(LOG_TAG, "Mute:OK uid3 = " + arrMsg[1]);
                                txReq = (TextView) findViewById(R.id.textReq3);
                                txReq.setText(getString(R.string.msg_muted));
                            }
                        }
                    }

                    //You can UnMute
                    if (getString(R.string.msg_can_unmute).equals(arrMsg[0])) {
                        TextView tx2 = (TextView) findViewById(R.id.textView2);
                        if (mUid==Integer.parseInt(arrMsg[1])){
                            doVoiceLocalMute(false);
                            sendChannelMessage(getString(R.string.msg_unmuted) + COLON + mUid);
                            if (String.valueOf(mUid).equals(tx2.getText().toString())){
                                Log.i(LOG_TAG, "UnMute:OK uid2 = " + arrMsg[1]);
                                txReq = (TextView) findViewById(R.id.textReq2);
                                txReq.setText(getString(R.string.msg_unmuted));
                            }else{
                                Log.i(LOG_TAG, "UnMute:OK uid3 = " + arrMsg[1]);
                                txReq = (TextView) findViewById(R.id.textReq3);
                                txReq.setText(getString(R.string.msg_unmuted));
                            }
                        }
                    }

                    //Muted
                    if (getString(R.string.msg_muted).equals(arrMsg[0])) {
                        TextView tx2 = (TextView) findViewById(R.id.textView2);
                        if (mUid==Integer.parseInt(arrMsg[1])){
                            doVoiceLocalMute(true);
                        }
                        if (arrMsg[1].equals(tx2.getText().toString())){
                            Log.i(LOG_TAG, "Mute:complete uid2 = " + arrMsg[1]);
                            txReq = (TextView) findViewById(R.id.textReq2);
                            txReq.setText(getString(R.string.msg_muted));
                        }else{
                            Log.i(LOG_TAG, "Mute:complete uid3 = " + arrMsg[1]);
                            txReq = (TextView) findViewById(R.id.textReq3);
                            txReq.setText(getString(R.string.msg_muted));
                        }
                    }


                    //UnMuted
                    if (getString(R.string.msg_unmuted).equals(arrMsg[0])) {
                        TextView tx2 = (TextView) findViewById(R.id.textView2);
                        if (mUid==Integer.parseInt(arrMsg[1])){
                            doVoiceLocalMute(false);
                        }
                        if (arrMsg[1].equals(tx2.getText().toString())){
                            Log.i(LOG_TAG, "UnMute:complete uid2 = " + arrMsg[1]);
                            txReq = (TextView) findViewById(R.id.textReq2);
                            txReq.setText(getString(R.string.msg_unmuted));
                        }else{
                            Log.i(LOG_TAG, "UnMute:complete uid3 = " + arrMsg[1]);
                            txReq = (TextView) findViewById(R.id.textReq3);
                            txReq.setText(getString(R.string.msg_unmuted));
                        }
                    }

                }

            });
        }

        @Override
        public void onMemberJoined(final RtmChannelMember member) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.i(LOG_TAG, "onMemberJoined member " + member.getUserId());
                 }
            });
        }

        @Override
        public void onMemberLeft(RtmChannelMember member) {
            Log.i(LOG_TAG, "onMemberLeft()");
        }
    }


    /**
     * API CALL: send message to a channel
     */
    private void sendChannelMessage(String content) {
        Log.i(LOG_TAG, "sendChannelMessage() " + content);

        RtmMessage message = mRtmClient.createMessage();
        message.setText(content);
        mRtmChannel.sendMessage(message, new ResultCallback<Void>() {

        @Override
        public void onSuccess(Void aVoid) {
            Log.i(LOG_TAG, "sendMessage onSuccess");
        }

        @Override
        public void onFailure(ErrorInfo errorInfo) {

            // refer to RtmStatusCode.ChannelMessageState for the message state
            final int errorCode = errorInfo.getErrorCode();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.i(LOG_TAG, "sendMessage onFailure " + errorCode);
                }
            });
        }
    });

}


}
