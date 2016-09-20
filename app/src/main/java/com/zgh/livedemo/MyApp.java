package com.zgh.livedemo;

import android.app.Application;
import android.util.Log;

import com.avos.avoscloud.AVOSCloud;
import com.avos.avoscloud.im.v2.AVIMClient;
import com.avos.avoscloud.im.v2.AVIMConversation;
import com.avos.avoscloud.im.v2.AVIMMessage;
import com.avos.avoscloud.im.v2.AVIMMessageHandler;
import com.avos.avoscloud.im.v2.AVIMMessageManager;
import com.avos.avoscloud.im.v2.messages.AVIMTextMessage;

/**
 * Created by zhuguohui on 2016/9/14.
 */
public class MyApp extends Application {
    public static AVIMClient mClient;

    @Override
    public void onCreate() {
        super.onCreate();
        AVOSCloud.initialize(this, Config.APP_ID,Config.APP_KEY);

    }


}
