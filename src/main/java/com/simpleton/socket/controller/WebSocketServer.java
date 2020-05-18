package com.simpleton.socket.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Author : Harry
 * Description : WebSocketServer
 * Date : 2020-05-15 11:14
 */
@Slf4j
@ServerEndpoint("/imserver/{userId}")
@Component
public class WebSocketServer {

    /**
     * 记录当前在线连接数, 设计成线程安全
     */
    private static volatile int onlineCount = 0;
    /**
     * concurrent包的线程安全Set，用来存放每个客户端对应的MyWebSocket对象。
     */
    private static ConcurrentHashMap<String, WebSocketServer> webSocketMap = new ConcurrentHashMap<>();
    /**
     * 与某个客户端的连接会话，需要通过它来给客户端发送数据
     */
    private Session session;

    /**
     * 接收userId
     */
    private String userId = "";

    public static void sendAll(String message) {

        ConcurrentHashMap.KeySetView<String, WebSocketServer> strings = webSocketMap.keySet();
        if (!strings.isEmpty()) {

            strings.forEach(a -> {
                try {
                    webSocketMap.get(a).sendMessage(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("userId") String userId) {

        this.session = session;
        this.userId = userId;
        if (webSocketMap.containsKey(userId)) {
            webSocketMap.remove(userId);
            webSocketMap.put(userId, this);
            //加入set中
        } else {
            webSocketMap.put(userId, this);
            //加入set中
            addOnlineCount();
            //在线数加1
        }

        log.info("用户连接 : {} , 当前在线人数为 : {} !", userId, getOnlineCount());
        try {
            sendMessage("连接成功");
        } catch (IOException e) {
            log.error("用户 : {} , 网络异常 !!!", userId);
        }
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose() {
        if (webSocketMap.containsKey(userId)) {
            webSocketMap.remove(userId);
            //从set中删除
            subOnlineCount();
        }
        log.info("用户退出 : {} , 当前在线人数 : {} !", userId, getOnlineCount());
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息
     */
    @OnMessage
    public void onMessage(String message, Session session) {

        log.info("用户消息 : {} , 报文 : {} !", userId, message);
        //可以群发消息
        //消息保存到数据库、redis
        if (!StringUtils.isEmpty(message)) {
            try {
                //解析发送的报文
                JSONObject jsonObject = JSON.parseObject(message);
                //追加发送人(防止串改)
                jsonObject.put("fromUserId", this.userId);
                String toUserId = jsonObject.getString("toUserId");
                //传送给对应toUserId用户的websocket
                if (!StringUtils.isEmpty(toUserId) && webSocketMap.containsKey(toUserId)) {

                    log.info("发送消息到 : {} , 消息内容为 : {} !", toUserId, jsonObject.get("contentText").toString());
                    webSocketMap.get(toUserId).sendMessage(jsonObject.get("contentText").toString());
                } else {
                    //否则不在这个服务器上，发送到mysql或者redis
                    log.error("请求的 userId : {} 不在该服务器上 !", userId);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {

        log.error("用户错误 : {} , 原因 : {} !", this.userId, error.getMessage());
        error.printStackTrace();
    }

    /**
     * 实现服务器主动推送
     */
    public void sendMessage(String message) throws IOException {

        this.session.getBasicRemote().sendText(message);
    }


    /**
     * 发送自定义消息
     */
    public static void sendInfo(String message, String userId) throws IOException {

        log.info("发送消息到: {} , 报文 : {}", userId, message);

        if (!StringUtils.isEmpty(userId) && webSocketMap.containsKey(userId)) {

            webSocketMap.get(userId).sendMessage(message);
        } else {
            log.error("用户 {} 不在线 !", userId);
        }
    }

    private static synchronized int getOnlineCount() {
        return onlineCount;
    }

    public static synchronized void addOnlineCount() {
        WebSocketServer.onlineCount ++;
    }

    public static synchronized void subOnlineCount() {
        WebSocketServer.onlineCount --;
    }
}
