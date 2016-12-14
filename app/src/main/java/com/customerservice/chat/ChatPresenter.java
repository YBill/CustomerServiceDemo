package com.customerservice.chat;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;

import com.customerservice.AppUtils;
import com.customerservice.Log;
import com.customerservice.chat.jsonmodel.ChatMsgEntity;
import com.customerservice.chat.jsonmodel.TextMsgEntity;
import com.customerservice.chat.model.FileEntity;
import com.customerservice.receiver.BroadCastCenter;
import com.customerservice.receiver.ReceiveMsgRunnable;
import com.ioyouyun.wchat.WeimiInstance;
import com.ioyouyun.wchat.message.ConvType;
import com.ioyouyun.wchat.message.WChatException;
import com.ioyouyun.wchat.protocol.MetaMessageType;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Bill on 2016/12/8.
 */

public class ChatPresenter {

    private Activity activity;
    private ChatView chatView;
    private MyInnerReceiver receiver;

    private List<ChatMsgEntity> chatMsgEntityList = new ArrayList<>();

    public ChatPresenter(ChatView chatView, Activity activity) {
        this.chatView = chatView;
        this.activity = activity;
        registerReceiver();
        ReceiveMsgRunnable runnable = new ReceiveMsgRunnable(activity);
        Thread msgThread = new Thread(runnable);
        msgThread.start();
    }

    /**
     * 注册本地广播
     */
    private void registerReceiver() {
        receiver = new MyInnerReceiver();
        BroadCastCenter.getInstance().registerReceiver(receiver, AppUtils.MSG_TYPE_RECEIVE, AppUtils.MSG_TYPE_SEND_FILE_PRO, AppUtils.MSG_TYPE_DOWNLOAD_IMAGE_FINISH);
    }

    /**
     * 注销广播
     */
    private void unregisterReceiver() {
        if (receiver != null)
            BroadCastCenter.getInstance().unregisterReceiver(receiver);
    }

    public void onDestroy() {
        unregisterReceiver();
    }

    public void sendImage(String filePath, String fileName){
        String msgId = WeimiInstance.getInstance().genLocalMsgId(AppUtils.CUSTOM_SERVICE_ID);

        int fileLength = 0;
        File file = new File(filePath);
        if (file.exists())
            fileLength = (int) file.length();

        Bitmap bitmap = AppUtils.compressPic(new File(filePath));
        String desPath = AppUtils.getChatImagePath(fileName);
        AppUtils.saveBitmap(bitmap, desPath);
        Bitmap thumbnailBitmap = AppUtils.getSmallBitmap(desPath);
        String thumbnailPath = AppUtils.getThumbnailPath(AppUtils.uid, msgId);
        AppUtils.saveBitmap(thumbnailBitmap, thumbnailPath);
        byte[] thumbnail = AppUtils.getByteByBitmap(bitmap);

        FileEntity fileEntity = new FileEntity();
        fileEntity.fileLength = fileLength;
        fileEntity.fileLocal = desPath;
        fileEntity.thumbnailPath = thumbnailPath;
        fileEntity.msgType = ChatMsgEntity.CHAT_TYPE_PEOPLE_SEND_IMAGE;
        fileEntity.time = System.currentTimeMillis();

        int desFileLength = 0;
        File desFile = new File(desPath);
        if (desFile.exists())
            desFileLength = (int) desFile.length();
        int thumbnailFileLength = 0;
        File thumbnailFile = new File(thumbnailPath);
        if (thumbnailFile.exists())
            thumbnailFileLength = (int) thumbnailFile.length();
        Log.logD("文件原图大小：" + fileLength + " |压缩后原图大小：" + desFileLength + " |缩略图大小：" + thumbnailFileLength);

        int sliceCount = 0;
        try {
            sliceCount = WeimiInstance.getInstance().sendFile(msgId, AppUtils.CUSTOM_SERVICE_ID, filePath, fileName, MetaMessageType.image, null, ConvType.single, null, thumbnail, 600);
        } catch (WChatException e) {
            e.printStackTrace();
        }
        if (sliceCount > 0) {
            List<Integer> list = new LinkedList<>();
            for (int i = 1; i <= sliceCount; i++) {
                list.add(i);
            }
            ReceiveMsgRunnable.fileSend.put(msgId, list);
            ReceiveMsgRunnable.fileSendCount.put(msgId, sliceCount);

            refreshUI(fileEntity);
        }
    }

    /**
     * 发送文本消息
     * @param text
     */
    public void sendText(String text) {
        String msgId = WeimiInstance.getInstance().genLocalMsgId(AppUtils.CUSTOM_SERVICE_ID);
        String sendMsg = AppUtils.encapsulateTextMsg(text);
//        String sendMsg = AppUtils.encapsulateTest();
        boolean result = false;
        try {
            result = WeimiInstance.getInstance().sendText(msgId, AppUtils.CUSTOM_SERVICE_ID, sendMsg, ConvType.single, null, 60);
        } catch (WChatException e) {
            e.printStackTrace();
        }
        if(result){
            TextMsgEntity entity = new TextMsgEntity();
            entity.content = text;
            entity.msgType = ChatMsgEntity.CHAT_TYPE_PEOPLE_SEND_TEXT;
            entity.time = System.currentTimeMillis();
            refreshUI(entity);
        }
    }

    /**
     * @param type 1:enter 2:leave
     * 发送富文本消息
     */
    public void sendMixedText(int type) {
        String msgId = WeimiInstance.getInstance().genLocalMsgId(AppUtils.CUSTOM_SERVICE_ID);
        try {
            WeimiInstance.getInstance().sendMixedText(msgId, AppUtils.CUSTOM_SERVICE_ID, AppUtils.encapsulateEnterOrLeaveMsg(type), ConvType.single, null, 60);
        } catch (WChatException e) {
            e.printStackTrace();
        }
    }

    private void refreshUI(ChatMsgEntity entity){
        chatMsgEntityList.add(entity);
        chatView.refreshList(chatMsgEntityList);
        chatView.scrollToPosition(chatMsgEntityList.size() - 1);
        chatView.clearInputMsg();
    }

    class MyInnerReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (AppUtils.MSG_TYPE_RECEIVE.equals(action)) {
                ChatMsgEntity entity = (ChatMsgEntity) intent.getSerializableExtra(AppUtils.TYPE_MSG);
                refreshUI(entity);
            }else if (AppUtils.MSG_TYPE_SEND_FILE_PRO.equals(action)) {
                String fileId = intent.getStringExtra(AppUtils.FILE_FILEID);
                int progress = intent.getIntExtra(AppUtils.FILE_PROGRESS, 0);
                Log.logD("发送进度：" + progress);
            }else if (AppUtils.MSG_TYPE_DOWNLOAD_IMAGE_FINISH.equals(action)) {
                int position = intent.getIntExtra(AppUtils.MSG_TYPE_POSITION, 0);
                FileEntity entity = (FileEntity) intent.getSerializableExtra(AppUtils.TYPE_MSG);
                chatMsgEntityList.set(position, entity);
                chatView.refreshList(chatMsgEntityList);

                Log.logD("下载大图完成,更新数据");
            }
        }
    }

}
