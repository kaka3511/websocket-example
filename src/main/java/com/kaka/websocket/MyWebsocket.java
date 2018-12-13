package com.kaka.websocket;

import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Description: TODO
 * @Author fuwei
 * @Date Created in  2018/12/13 11:18
 */
@ServerEndpoint(value = "/myWebsocket")
@Component
public class MyWebsocket {
    private static Logger logger = LoggerFactory.getLogger(MyWebsocket.class);

    //在线人数统计
    private static volatile AtomicInteger onlineCount = new AtomicInteger(0);

    //线程安全的set,用于存放websocket对象
    private static Map<String, MyWebsocket> webSocketServers = new ConcurrentHashMap();

    //连接会话
    private Session session;

    //当前用户
    private String account;

    /**
     * 建立连接
     *
     * @param session
     */
    @OnOpen
    public void onOpen(Session session){
        addOnlineCount();
        String token = session.getQueryString();
        this.account = token;
        this.session = session;
        webSocketServers.put(token, this);
        logger.info(String.format("用户建立连接,token: %s", token));
        logger.info(String.format("当前用户数：%s", onlineCount));
        new Thread(new MyTask()).start();
    }

    /**
     * 断开连接
     */
    @OnClose
    public void onClose() {
        subOnlineCount();
        logger.info(String.format("用户断开连接,token: %s", this.account));
        logger.info(String.format("当前用户数：%s", onlineCount));
        webSocketServers.remove(account);
    }


    /**
     * 接收客户端消息
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        logger.info(String.format("来自客户端【%s】的消息:【%s】", session, message));
    }

    /**
     * 增加在线人数
     */
    private static synchronized void addOnlineCount() {
        onlineCount.incrementAndGet();
    }

    /**
     * 减少在线人数
     */
    private static synchronized void subOnlineCount() {
        onlineCount.decrementAndGet();
    }


    private static class MyTask implements Runnable {

        @Override
        public void run() {
            while (true) {
                try {
                    TimeUnit.SECONDS.sleep(5L);
                    if (webSocketServers.size() > 0) {
                        sendMessageToAll(JSONObject.toJSONString("服务端发送内容：【something...】"));
                        logger.info("服务端发送内容：【something...】");
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    /**
     * 广播消息
     *
     * @param message
     * @throws Exception
     */
    private static void sendMessageToAll(String message) {
        webSocketServers.forEach((key, server) -> {
            try {
                server.sendMessage(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * 推送消息给前端
     *
     * @param message
     * @throws Exception
     */
    private void sendMessage(String message) throws Exception {
        this.session.getBasicRemote().sendText(message);
    }

}
