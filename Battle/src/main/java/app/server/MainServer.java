package app.server;

import app.mechanics.GameServer;
import io.javalin.Javalin;
import io.javalin.websocket.WsContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MainServer {
    private Javalin server;
    private Map<WsContext, String> userMap = new ConcurrentHashMap();
    private Map<Integer, GameServer> gameMap = new ConcurrentHashMap();
    private int nextUserId = 1;
    private int nextGameId = 1;

    public MainServer(int port, String address) {

        server = Javalin.create(config -> {
            config.addStaticFiles("/public");
        }).start(port);
        server.ws(address, ws -> {
            ws.onConnect(ctx -> {
                userMap.put(ctx, "User" + nextUserId++);
            });
            ws.onClose(ctx -> {
                userMap.remove(ctx);
            });
            ws.onMessage(ctx -> {
                processCommand(ctx, ctx.message());
            });
        });
    }

    private void processCommand(WsContext ctx, String command) {
        String[] commandParts = command.split("\\s");
        /*
        /start
        + заделка на чат и статистику
         */
        switch (commandParts[0]) {
            case "/start":
                for (Map.Entry<Integer, GameServer> serv : gameMap.entrySet()) {
                    if (serv.getValue().isFree()) {
                        ctx.send("/connect " + serv.getKey());
                        return;
                    }
                }
                int gameId = nextGameId++;
                gameMap.put(gameId, new GameServer(gameId, this));
                ctx.send("/connect " + gameId);
            default:
                break;
        }
    }

    public void closeGameServer(int id) {
        gameMap.remove(id);
    }
}
