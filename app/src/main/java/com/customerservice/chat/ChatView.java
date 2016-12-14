package com.customerservice.chat;

import com.customerservice.chat.jsonmodel.ChatMsgEntity;
import com.customerservice.chat.model.FileEntity;

import java.util.List;

/**
 * Created by Bill on 2016/12/8.
 */

public interface ChatView {

    void refreshList(List<ChatMsgEntity> list);

    void scrollToPosition(int position);

    void clearInputMsg();

}
