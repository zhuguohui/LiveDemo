package com.zgh.livedemo;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.KeyListener;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.avos.avoscloud.im.v2.AVIMClient;
import com.avos.avoscloud.im.v2.AVIMConversation;
import com.avos.avoscloud.im.v2.AVIMException;
import com.avos.avoscloud.im.v2.AVIMMessage;
import com.avos.avoscloud.im.v2.AVIMMessageHandler;
import com.avos.avoscloud.im.v2.AVIMMessageManager;
import com.avos.avoscloud.im.v2.callback.AVIMClientCallback;
import com.avos.avoscloud.im.v2.callback.AVIMConversationCallback;
import com.avos.avoscloud.im.v2.callback.AVIMConversationCreatedCallback;
import com.avos.avoscloud.im.v2.messages.AVIMTextMessage;
import com.zgh.livedemo.view.BarrageView;
import com.zgh.livedemo.view.MyVideoView;

import java.util.Arrays;

import io.vov.vitamio.LibsChecker;
import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.utils.Log;
import io.vov.vitamio.widget.MediaController;
import io.vov.vitamio.widget.VideoView;

public class MainActivity extends Activity {

    private MyVideoView mVideoView;
    private String path;
    private AVIMConversation conv;
    private RoomMessageHandler roomMessageHandler;
    private EditText et_send;
    BarrageView barrageView;
    private LinearLayout ll_room;
    private View layout_video, layout_loading;
    private TextView tv_present;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!LibsChecker.checkVitamioLibs(this))
            return;
        setContentView(R.layout.activity_main);
        layout_loading = findViewById(R.id.layout_loading);
        layout_video = findViewById(R.id.layout_video);
        tv_present = (TextView) findViewById(R.id.tv_present);
        findViewById(R.id.btn_fullscreen).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                fullScreen();
            }
        });
        barrageView = (BarrageView) findViewById(R.id.containerView);
        ll_room = (LinearLayout) findViewById(R.id.ll_room);
        et_send = (EditText) findViewById(R.id.et_send);
        mVideoView = (MyVideoView) findViewById(R.id.vitamio_videoView);
        path = Config.VIDEO_URL;
        mVideoView.setVideoPath(path);
        MediaController mediaController = (MediaController) findViewById(R.id.mediacontroller);
        //    mediaController.setAnchorView(mVideoView);
        mVideoView.setMediaController(mediaController);
        mVideoView.requestFocus();
        mVideoView.setOnInfoListener(new MediaPlayer.OnInfoListener() {
            public boolean onInfo(MediaPlayer mp, int what, int extra) {
                if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
                    layout_loading.setVisibility(View.VISIBLE);
                    android.util.Log.i("zzz", "onStart");

                } else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END) {
                    //此接口每次回调完START就回调END,若不加上判断就会出现缓冲图标一闪一闪的卡顿现象
                    android.util.Log.i("zzz", "onEnd");
                    layout_loading.setVisibility(View.GONE);
                    //   mp.start();
                    mVideoView.start();
                }
                return true;
            }
        });
        mVideoView.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(MediaPlayer mp, int percent) {
                if (!mp.isPlaying()) {
                    layout_loading.setVisibility(View.VISIBLE);
                    tv_present.setText("正在缓冲" + percent + "%");
                } else {
                    layout_loading.setVisibility(View.GONE);
                }

            }
        });
        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mediaPlayer.setPlaybackSpeed(1.0f);
            }
        });
        mVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                android.util.Log.i("zzz", "onError what=" + what + " extra=" + extra);
                tv_present.setText("加载失败");
                return true;
            }
        });

        roomMessageHandler = new RoomMessageHandler();
        join();

        et_send.setOnKeyListener(new MyKeyListener());

    }


    private class MyKeyListener implements View.OnKeyListener {
        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                sendMsg();
                return true;
            }
            return false;
        }
    }

    private void sendMsg() {
        final String msg = et_send.getText().toString();
        if (TextUtils.isEmpty(msg)) {
            Toast.makeText(this, "内容不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        if (conv != null) {
            AVIMTextMessage message = new AVIMTextMessage();
            message.setText(msg);
            conv.sendMessage(message, new AVIMConversationCallback() {
                @Override
                public void done(AVIMException e) {
                    if (e == null) {
                        et_send.setText("");
                        addMsg(msg);
                    } else {
                        Toast.makeText(MainActivity.this, "发送失败", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void join() {
        MyApp.mClient.open(new AVIMClientCallback() {

            @Override
            public void done(AVIMClient client, AVIMException e) {
                if (e == null) {
                    //登录成功
                    conv = client.getConversation(Config.CONVERSATION_ID);
                    conv.join(new AVIMConversationCallback() {
                        @Override
                        public void done(AVIMException e) {
                            if (e == null) {
                                //加入成功
                                Toast.makeText(MainActivity.this, "加入聊天室成功", Toast.LENGTH_SHORT).show();
                                et_send.setEnabled(true);
                            } else {
                                Toast.makeText(MainActivity.this, "加入聊天室失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                                et_send.setEnabled(false);
                                android.util.Log.i("zzz", "加入聊天室失败 :" + e.getMessage());
                            }
                        }
                    });
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        AVIMMessageManager.registerMessageHandler(AVIMTextMessage.class, roomMessageHandler);
    }

    @Override
    protected void onPause() {
        super.onPause();
        AVIMMessageManager.unregisterMessageHandler(AVIMTextMessage.class, roomMessageHandler);
    }

    public class RoomMessageHandler extends AVIMMessageHandler {
        //接收到消息后的处理逻辑
        @Override
        public void onMessage(AVIMMessage message, AVIMConversation conversation, AVIMClient client) {
            if (message instanceof AVIMTextMessage) {
                String info = ((AVIMTextMessage) message).getText();
                //添加消息到屏幕
                addMsg(info);
            }
        }

    }

    private void addMsg(String msg) {
        TextView textView = new TextView(MainActivity.this);
        textView.setText(msg);
        ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(5, 10, 5, 10);
        textView.setLayoutParams(params);
        ll_room.addView(textView, 0);
        barrageView.addMessage(msg);
    }

    private void fullScreen() {
        if (isScreenOriatationPortrait(this)) {// 当屏幕是竖屏时
            full(true);
            // 点击后变横屏
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);// 设置当前activity为横屏
            // 当横屏时 把除了视频以外的都隐藏
            //隐藏其他组件的代码
            ll_room.setVisibility(View.GONE);
            et_send.setVisibility(View.GONE);
            int width = getResources().getDisplayMetrics().widthPixels;
            int height = getResources().getDisplayMetrics().heightPixels;
            layout_video.setLayoutParams(new LinearLayout.LayoutParams(height, width));
            mVideoView.setLayoutParams(new RelativeLayout.LayoutParams(height, width));


        } else {
            full(false);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);// 设置当前activity为竖屏
            //显示其他组件
            ll_room.setVisibility(View.VISIBLE);
            et_send.setVisibility(View.VISIBLE);
            int width = getResources().getDisplayMetrics().heightPixels;
            int height = (int) (width * 9.0 / 16);
            layout_video.setLayoutParams(new LinearLayout.LayoutParams(width, height));
            mVideoView.setLayoutParams(new RelativeLayout.LayoutParams(width, height));

        }
    }

    //动态隐藏状态栏
    private void full(boolean enable) {
        if (enable) {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
            getWindow().setAttributes(lp);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        } else {
            WindowManager.LayoutParams attr = getWindow().getAttributes();
            attr.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().setAttributes(attr);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }
    }


    /**
     * 返回当前屏幕是否为竖屏。
     *
     * @param context
     * @return 当且仅当当前屏幕为竖屏时返回true, 否则返回false。
     */
    public static boolean isScreenOriatationPortrait(Context context) {
        return context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;

    }
}
