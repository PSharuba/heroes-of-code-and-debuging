package app;

//import app.util.HerokuUtil;

import io.javalin.Javalin;
import io.javalin.websocket.WsContext;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONObject;

import static j2html.TagCreator.*;

public class Chat {

    private static Map<WsContext, String> userUsernameMap = new ConcurrentHashMap<>();
    private static int nextUserNumber = 1; // Assign to username for next connecting user

    public static void main(String[] args) {
        Javalin app = Javalin.create(config -> {
            config.addStaticFiles("/public");
        }).start(8080 /*HerokuUtil.getHerokuAssignedPort()*/);

        app.ws("/chat", ws -> {
            ws.onConnect(ctx -> {
                String username = "User" + nextUserNumber++;
                userUsernameMap.put(ctx, username);
                broadcastMessage("Server", (username + " joined the chat"));
            });
            ws.onClose(ctx -> {
                String username = userUsernameMap.get(ctx);
                userUsernameMap.remove(ctx);
                broadcastMessage("Server", (username + " left the chat"));
            });
            ws.onMessage(ctx -> {
                if (ctx.message().charAt(0) == '@') {
                    String[] split = ctx.message().substring(1).split("\\s", 2);
                    broadcastPrivateMessage(userUsernameMap.get(ctx), split[0], ctx.message());
                } else
                    broadcastMessage(userUsernameMap.get(ctx), ctx.message());
            });
        });
    }

    // Sends a message from one user to all users, along with a list of current usernames
    private static void broadcastMessage(String sender, String message) {
        userUsernameMap.keySet().stream().filter(ctx -> ctx.session.isOpen()).forEach(session -> {
            session.send(
                    new JSONObject()
                            .put("userMessage", createHtmlMessageFromSender(sender, message))
                            .put("userlist", userUsernameMap.values()).toString()
            );
        });
    }

    private static void broadcastPrivateMessage(String sender, String toUser, String message) {
        userUsernameMap.entrySet().stream().filter(entry -> entry.getKey().session.isOpen())
                .filter(entry -> entry.getValue().equals(toUser)).forEach(session -> {
            session.getKey().send(
                    new JSONObject()
                            .put("userMessage", createHtmlMessageFromSender(sender, message))
                            .put("userlist", userUsernameMap.values()).toString()
            );
        });
        userUsernameMap.entrySet().stream().filter(entry -> entry.getKey().session.isOpen())
                .filter(entry -> entry.getValue().equals(sender)).forEach(session -> {
            session.getKey().send(
                    new JSONObject()
                            .put("userMessage", createHtmlMessageFromSender(sender, message))
                            .put("userlist", userUsernameMap.values()).toString()
            );
        });
    }

    // Builds a HTML element with a sender-name, a message, and a timestamp
    private static String createHtmlMessageFromSender(String sender, String message) {
        return article(
                b(sender + " says:"),
                span(attrs(".timestamp"), new SimpleDateFormat("HH:mm:ss").format(new Date())),
                p(message)
        ).render();
    }

}
