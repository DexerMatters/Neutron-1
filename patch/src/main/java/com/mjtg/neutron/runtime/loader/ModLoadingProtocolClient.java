package com.mjtg.neutron.runtime.loader;


import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;

public class ModLoadingProtocolClient extends WebSocketClient {

    private boolean inProgress;

    private Consumer<String> stringMessageConsumer;

    private Consumer<ByteBuffer> bytesMessageConsumer;

    public ModLoadingProtocolClient(URI serverUri) {
        super(serverUri);
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {}

    @Override
    public void onMessage(String message) {
        if(stringMessageConsumer!=null) {
            stringMessageConsumer.accept(message);
        }
    }

    @Override
    public void onMessage(ByteBuffer bytes) {
        if(bytesMessageConsumer != null){
            bytesMessageConsumer.accept(bytes);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Log.e("Neutron-ModLoader", "connection closed");
    }

    @Override
    public void onError(Exception ex) {
        Log.e("Neutron-ModLoader", "error while communicating with server", ex);
    }

    public Future<List<String>> fetchModList() {
        if(inProgress) {
            throw new IllegalStateException("the client does not support concurrent usage!");
        }
        CompletableFuture<List<String>> fut = new CompletableFuture<>();

        //handle response
        inProgress = true;
        stringMessageConsumer = str -> {
            inProgress = false;
            stringMessageConsumer = null;
            try {
                final JSONArray json = new JSONArray(str);
                List<String> resp = new ArrayList<>();
                for (int i=0;i<json.length();i++) {
                    resp.add(json.getString(i));
                }
            } catch(Exception e) {
                fut.completeExceptionally(e);
            }
        };

        //send request
        try {
            send(
                    new JSONObject()
                        .put("type", "listMods")
                    .toString()
            );
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        return fut;
    }

    public Future<ByteBuffer> fetchMod(String str) {
        if(inProgress) {
            throw new IllegalStateException("the client does not support concurrent usage!");
        }
        CompletableFuture<ByteBuffer> fut = new CompletableFuture<>();

        //handle response
        inProgress = true;
        bytesMessageConsumer = bytes -> {
            inProgress = false;
            stringMessageConsumer = null;
            fut.complete(bytes);
        };

        //send request
        try {
            send(
                    new JSONObject()
                            .put("type", "fetchMod")
                            .put("modName", str)
                            .toString()
            );
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        return fut;
    }
}
