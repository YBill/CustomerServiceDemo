package com.customerservice.chat.jsonmodel;

import java.io.Serializable;

/**
 * Created by Bill on 2016/12/8.
 */

public class ActionMsgEntity extends ChatMsgEntity implements Serializable {

    public String content;
    public String param; // param存一个json，客户端不需要，原样传给客服

    public ActionMsgEntity() {
        type = "action";
        msgType = CHAT_TYPE_ROBOT_TEXT;
    }

}
