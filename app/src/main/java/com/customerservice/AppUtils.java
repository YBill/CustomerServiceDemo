package com.customerservice;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.provider.Settings;
import android.util.*;
import android.widget.Toast;

import com.customerservice.chat.jsonmodel.ActionMsgEntity;
import com.customerservice.chat.jsonmodel.CardMsgEntity;
import com.customerservice.chat.jsonmodel.ChatMsgEntity;
import com.customerservice.chat.jsonmodel.LinkMsgEntity;
import com.customerservice.chat.jsonmodel.TextMsgEntity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Bill on 2016/12/8.
 */

public class AppUtils {

    public static String CLIENT_ID = "1-20521-1b766ad17389c94e1dc1f2615714212a-andriod";
    public static String SECRET = "d5cf0a5812b4424f582ded05937e4387";
    public static String CLIENT_ID_TEST = "1-20142-2e563db99a8ca41df48973b0c43ea50a-andriod";
    public static String SECRET_TEST = "ace518dab1fde58eacb126df6521d34c";

    public static boolean isOnlinePlatform = true;
    public static Context mAppContext;
    public static String uid; // 用户ID

    public static String CUSTOM_SERVICE_ID = "271576"; // 魅族：271576 小米：238973 nexus:176329

    // 客服：549341
    // 线上 ：  魅族：271576 小米：238973 nexus:176329
    // 测试版： 魅族：539578 nexus：543951

    public static final String MSG_TYPE_RECEIVE = "msg_type_receive"; // 收到消息
    public static final String TYPE_MSG = "type_msg";
    public static final String MSG_TYPE_SEND_FILE_PRO = "msg_type_send_file_pro"; // 上传文件进度
    public static final String MSG_TYPE_DOWNLOAD_FILE_PRO = "msg_type_download_file_pro"; // 下载文件进度
    public static final String FILE_FILEID = "file_fileid";
    public static final String FILE_PROGRESS = "file_progress";

    public static final String MSG_TYPE_DOWNLOAD_IMAGE_FINISH = "msg_type_download_image_finish"; // 收到大图后更新聊天数据,避免重复下载
    public static final String MSG_TYPE_POSITION = "msg_type_position"; // 收到大图后更新聊天数据,避免重复下载

    /**
     * 获取Android Id
     *
     * @return
     */
    public static String generateOpenUDID(Activity activity) {
        // Try to get the ANDROID_ID
        String OpenUDID = Settings.Secure.getString(activity.getContentResolver(), Settings.Secure.ANDROID_ID);
        if (OpenUDID == null || OpenUDID.equals("9774d56d682e549c") | OpenUDID.length() < 15) {
            // if ANDROID_ID is null, or it's equals to the GalaxyTab generic
            // ANDROID_ID or bad, generates a new one
            final SecureRandom random = new SecureRandom();
            OpenUDID = new BigInteger(64, random).toString(16);
        }
        return OpenUDID;
    }

    /**
     * Toast
     *
     * @param msg
     */
    private static Toast toast;

    public static void toastMessage(String msg) {
        if (toast == null)
            toast = Toast.makeText(mAppContext, msg, Toast.LENGTH_SHORT);
        else
            toast.setText(msg);
        toast.show();
    }

    //////////////////////////////////////客服JSON消息处理/////////////////////////////////////////

    private static final String TYPE = "type";
    private static final String TEXT = "text";
    private static final String LINK = "link";
    private static final String ACTION = "action";
    private static final String EVENT = "event";
    private static final String CARD = "card";
    private static final String PARAM = "param";
    private static final String CONTENT = "content";
    private static final String URL = "url";

    private static final String SORT = "sort";
    private static final String ENTER_KEY = "entercs";
    private static final String LEAVE_KEY = "leavecs";
    private static final String FROM = "from";
    private static final String ROOM = "room";

    /**
     * 封装text消息
     *
     * @param text
     * @return
     */
    public static String encapsulateTextMsg(String text) {
        JSONObject object = new JSONObject();
        try {
            object.put(TYPE, TEXT);
            object.put(CONTENT, text);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return object.toString();
    }

    /**
     * 封装进入和离开客服界面发送给Gateway的命令
     *
     * @param sort 1:enter 2:leave
     * @return
     */
    public static String encapsulateEnterOrLeaveMsg(int sort) {
        JSONObject object = new JSONObject();
        try {
            object.put(TYPE, EVENT);
            JSONObject paramObj = new JSONObject();
            if (1 == sort)
                paramObj.put(SORT, ENTER_KEY);
            else if (2 == sort)
                paramObj.put(SORT, LEAVE_KEY);
            paramObj.put(FROM, ROOM);
            object.put(PARAM, paramObj);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return object.toString();
    }

    /**
     * 封装Action消息
     *
     * @param actionMsgEntity
     * @return
     */
    public static String encapsulateClickMsg(ActionMsgEntity actionMsgEntity) {
        JSONObject object = new JSONObject();
        try {
            object.put(TYPE, ACTION);
            object.put(CONTENT, actionMsgEntity.content);
            JSONObject paramObj = new JSONObject(actionMsgEntity.param);
            object.put(PARAM, paramObj);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return object.toString();
    }

    /**
     * 解析机器人消息
     *
     * @param json
     * @return
     */
    public static ChatMsgEntity parseRobotMsg(String json) {
        try {
            JSONObject object = new JSONObject(json);
            if (object.has(TYPE)) {
                String type = object.getString(TYPE);
                if (TEXT.equals(type)) {
                    String content = object.getString(CONTENT);
                    TextMsgEntity textMsgEntity = new TextMsgEntity();
                    textMsgEntity.content = content;
                    return textMsgEntity;
                } else if (LINK.equals(type)) {
                    String content = object.getString(CONTENT);
                    String url = object.getString(URL);
                    LinkMsgEntity linkMsgEntity = new LinkMsgEntity();
                    linkMsgEntity.content = content;
                    linkMsgEntity.url = url;
                    return linkMsgEntity;
                } else if (ACTION.equals(type)) {
                    String content = object.getString(CONTENT);
                    JSONObject paramObj = object.getJSONObject(PARAM);
                    ActionMsgEntity actionMsgEntity = new ActionMsgEntity();
                    actionMsgEntity.content = content;
                    actionMsgEntity.param = paramObj.toString();
                    return actionMsgEntity;
                } else if (CARD.equals(type)) {
                    CardMsgEntity cardMsgEntity = new CardMsgEntity();
                    List<ChatMsgEntity> list = new ArrayList<>();
                    JSONArray array = object.getJSONArray(CONTENT);
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject arrayObj = array.getJSONObject(i);
                        list.add(parseRobotMsg(arrayObj.toString()));
                    }
                    cardMsgEntity.content = list;
                    return cardMsgEntity;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 判断是object or array
     *
     * @param json
     */
    private void isObjectOrArray(String json) {
        JSONTokener tokener = new JSONTokener(json);
        try {
            Object object = tokener.nextValue();
            if (object instanceof JSONObject) {
                // object
            } else if (object instanceof JSONArray) {
                // array
            } else {
                // json格式错误
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static String encapsulateTest() {
        JSONObject object = new JSONObject();
        try {
            object.put(TYPE, CARD);
            JSONArray array = new JSONArray();


            JSONObject textObj = new JSONObject();
            textObj.put(TYPE, TEXT);
            textObj.put(CONTENT, "您好，请选择一下问题，点击即可获取答案哦！");
            array.put(textObj);


            JSONObject cardObj2 = new JSONObject();
            cardObj2.put(TYPE, CARD);
            JSONArray cardArray2 = new JSONArray();

            JSONObject actionObj = new JSONObject();
            actionObj.put(TYPE, ACTION);
            actionObj.put(CONTENT, "付款后什么时候可以发货呢？");
            JSONObject paramObj = new JSONObject();
            paramObj.put("id", 1);
            paramObj.put("time", System.currentTimeMillis());
            actionObj.put(PARAM, paramObj);
            cardArray2.put(actionObj);

            JSONObject actionObj2 = new JSONObject();
            actionObj2.put(TYPE, ACTION);
            actionObj2.put(CONTENT, "请问发什么快递呢？");
            JSONObject paramObj2 = new JSONObject();
            paramObj2.put("id", 1);
            paramObj2.put("time", System.currentTimeMillis());
            actionObj2.put(PARAM, paramObj2);
            cardArray2.put(actionObj2);

            JSONObject actionObj3 = new JSONObject();
            actionObj3.put(TYPE, ACTION);
            actionObj3.put(CONTENT, "可以指定某个快递吗？");
            JSONObject paramObj3 = new JSONObject();
            paramObj3.put("id", 1);
            paramObj3.put("time", System.currentTimeMillis());
            actionObj3.put(PARAM, paramObj3);
            cardArray2.put(actionObj3);

            JSONObject actionObj4 = new JSONObject();
            actionObj4.put(TYPE, ACTION);
            actionObj4.put(CONTENT, "发货地址写错了，可以更改吗？");
            JSONObject paramObj4 = new JSONObject();
            paramObj4.put("id", 1);
            paramObj4.put("time", System.currentTimeMillis());
            actionObj4.put(PARAM, paramObj4);
            cardArray2.put(actionObj4);

            cardObj2.put(CONTENT, cardArray2);
            array.put(cardObj2);


            JSONObject cardObj3 = new JSONObject();
            cardObj3.put(TYPE, CARD);
            JSONArray cardArray3 = new JSONArray();

            JSONObject textObj3 = new JSONObject();
            textObj3.put(TYPE, TEXT);
            textObj3.put(CONTENT, "没有我要的答案，请选择");
            cardArray3.put(textObj3);

            JSONObject actionObj5 = new JSONObject();
            actionObj5.put(TYPE, ACTION);
            actionObj5.put(CONTENT, "接人工客服");
            JSONObject paramObj5 = new JSONObject();
            paramObj5.put("id", 1);
            paramObj5.put("time", System.currentTimeMillis());
            actionObj5.put(PARAM, paramObj5);
            cardArray3.put(actionObj5);

            cardObj3.put(CONTENT, cardArray3);
            array.put(cardObj3);


            JSONObject cardObj4 = new JSONObject();
            cardObj4.put(TYPE, CARD);
            JSONArray cardArray4 = new JSONArray();

            JSONObject textObj4 = new JSONObject();
            textObj4.put(TYPE, TEXT);
            textObj4.put(CONTENT, "商品详情页：");
            cardArray4.put(textObj4);

            JSONObject actionObj6 = new JSONObject();
            actionObj6.put(TYPE, LINK);
            actionObj6.put(CONTENT, "点击我！！！");
            actionObj6.put(URL, "http://17youyun.com/");
            cardArray4.put(actionObj6);

            cardObj4.put(CONTENT, cardArray4);
            array.put(cardObj4);

            object.put(CONTENT, array);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return object.toString();
    }

    ////////////////////////////图片简单压缩处理///////////////////////

    // 保存byte[]图片
    public static void saveImg(byte[] byteImg, String filePath) {
        Bitmap bmp = null;
        try {
            bmp = BitmapFactory.decodeByteArray(byteImg, 0, byteImg.length);
            saveImg(bmp, filePath);

        } catch (Exception e) {
            e.getMessage();
        } finally {
            if (bmp != null) {
                bmp.recycle();
                bmp = null;
            }
        }

    }

    public static void saveImg(Bitmap bmp, String filePath) {
        saveImg(bmp, filePath, 100);
    }

    public static void saveImg(Bitmap bmp, String filePath, int quality) {
        File file = new File(filePath);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            if (fos != null) {
                bmp.compress(Bitmap.CompressFormat.PNG, quality, fos);
                fos.flush();
            }

        } catch (Exception e) {
            android.util.Log.e("FileUtil", e.getMessage());
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    android.util.Log.e("FileUtil", e.getMessage());
                }
                fos = null;
            }
        }
    }

    /**
     * 将Bitmpa转化为byte[]
     *
     * @param bitmap
     * @return
     */
    public static byte[] getByteByBitmap(Bitmap bitmap) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
            byte[] result = output.toByteArray();
            return result;
        } catch (Exception e) {
            e.getMessage();
        }
        return null;
    }

    /**
     * 将Bitmap保存到本地
     * @param bitmap
     * @param savePath
     */
    public static void saveBitmap(Bitmap bitmap, String savePath) {
        try {
            FileOutputStream outputStream = new FileOutputStream(savePath);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);

            outputStream.flush();
            outputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 根据路径获得图片并压缩，返回bitmap用于显示
     *
     * @param filePath
     * @return
     */
    public static Bitmap getSmallBitmap(String filePath) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, 150, 150);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;

        return BitmapFactory.decodeFile(filePath, options);
    }

    /**
     * 计算图片的缩放值
     *
     * @param options
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        return inSampleSize;
    }

    public static Bitmap compressPic(File file) {
        try {
            // BitmapFactory options to downsize the image
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            o.inSampleSize = 6;
            // factor of downsizing the image

            FileInputStream inputStream = new FileInputStream(file);
            //Bitmap selectedBitmap = null;
            BitmapFactory.decodeStream(inputStream, null, o);
            inputStream.close();

            // The new size we want to scale to
            final int REQUIRED_SIZE = 75;

            // Find the correct scale value. It should be the power of 2.
            int scale = 1;
            while (o.outWidth / scale / 2 >= REQUIRED_SIZE &&
                    o.outHeight / scale / 2 >= REQUIRED_SIZE) {
                scale *= 2;
            }

            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            inputStream = new FileInputStream(file);

            Bitmap selectedBitmap = BitmapFactory.decodeStream(inputStream, null, o2);
            inputStream.close();

            return selectedBitmap;
        } catch (Exception e) {
            return null;
        }
    }

    /////////////////////////////////////图片收发路径管理/////////////////////////////////////

    public static String getAppRootPath() {
        String filePath = "/kefu";
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            filePath = Environment.getExternalStorageDirectory() + filePath;
        } else {
            filePath = mAppContext.getApplicationContext().getCacheDir() + filePath;
        }
        File file = new File(filePath);
        if (!file.exists()) {
            file.mkdirs();
        }
        file = null;
        return filePath;
    }

    public static String getImageRootPath() {
        String filePath = getAppRootPath() + "/image/";
        File file = new File(filePath);
        if (!file.exists()) {
            file.mkdirs();
        }
        file = null;
        return filePath;
    }

    public static String getCameraPath() {
        String filePath = getImageRootPath() + "/camera/";
        File file = new File(filePath);
        if (!file.isDirectory()) {
            file.mkdirs();
        }
        file = null;
        return filePath;
    }

    public static String getThumbnailImgRootPath(String uid) {
        String filePath = getImageRootPath() + "/thumbnail/" + uid + "/";
        File file = new File(filePath);
        if (!file.exists()) {
            file.mkdirs();
        }
        file = null;

        return filePath;
    }

    public static String getThumbnailPath(String uid, String filename) {
        String path = getThumbnailImgRootPath(uid) + filename + ".png";
        return path;
    }

    public static String getChatImageRootPath() {
        String path = getImageRootPath() + "chat_img/";
        File file = new File(path);
        if (!file.exists()) {
            file.mkdirs();
        }
        return path;
    }

    public static String getChatImagePath(String fileName) {
        String path = getChatImageRootPath() + fileName;
        return path;
    }

}
