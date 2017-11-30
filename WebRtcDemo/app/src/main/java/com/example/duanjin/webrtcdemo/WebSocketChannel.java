package com.example.duanjin.webrtcdemo;

import android.text.TextUtils;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by duanjin on 5/11/17.
 */

public class WebSocketChannel extends WebSocketClient{


    private IPeerStateListener mStateListener;
    private String mClientId;


    public WebSocketChannel(URI serverURI) {
        super(serverURI);
    }

    public void setPeerStateListener(IPeerStateListener stateListener) {
        this.mStateListener = stateListener;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {

    }

    @Override
    public void onMessage(String message) {
        if (!TextUtils.isEmpty(message)) {
            try {
                JSONObject data = new JSONObject(message);
                String type = data.optString("type");
                switch (type) {
                    case "id":
                        //建立连接，确定client id
                        mClientId = data.optString("clientId");
                        if (mStateListener != null) {
                            mStateListener.onReceiveClientId(mClientId);
                        }

                        break;
                    case "users":
                        //收到用户列表
                        JSONArray users = data.optJSONArray("list");
                        List<String> usersList = new ArrayList<>();
                        for (int i = 0; i < users.length(); ++i) {
                            usersList.add((String)users.get(i));
                        }

                        if (mStateListener != null) {
                            mStateListener.onReceiveUsers(usersList);
                        }
                        break;
                    case "offer":
                        String peerName = data.optString("from");
                        String sdpDes = data.optString("sdp");
                        SessionDescription sdp = new SessionDescription(SessionDescription.Type.OFFER, sdpDes);
                        if (mStateListener != null) {
                            mStateListener.onReceivePeerOffer(peerName, sdp);
                        }

                        break;
                    case "answer":
                        String sdpDes2 = data.optString("sdp");
                        SessionDescription answerSdp = new SessionDescription(SessionDescription.Type.ANSWER, sdpDes2);
                        if (mStateListener != null) {
                            mStateListener.onReceivePeerAnswer(answerSdp);
                        }
                        break;
                    case "icecandidate":
                        IceCandidate candidate = new IceCandidate(data.optString("id"), Integer.valueOf(data.optString("index")), data.optString("sdp"));
                        if (mStateListener != null) {
                            mStateListener.onReceiveIceCandidate(candidate);
                        }
                        break;
                    case "removecandidate":
                        JSONArray array = data.getJSONArray("array");
                        IceCandidate[] candidates = new IceCandidate[array.length()];
                        for (int i = 0; i < candidates.length; ++i) {
                            JSONObject object = (JSONObject) array.get(i);
                            candidates[i] = new IceCandidate(object.optString("id"), object.optInt("index"), object.optString("sdp"));
                        }
                        if (mStateListener != null) {
                            mStateListener.onReceiveIceCandidatesRemove(candidates);
                        }
                        break;

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    public void onClose(int code, String reason, boolean remote) {

    }

    @Override
    public void onError(Exception ex) {

    }

    public void send(Map<String, String> data) {
        data.putAll(generateSysParam());

        JSONObject object = new JSONObject(data);
        send(object.toString());
    }

    private Map<String, String> generateSysParam() {
        Map<String, String> basic = new HashMap<>();
        if (!TextUtils.isEmpty(mClientId)) {
            basic.put("clientId", mClientId);
        }
        return basic;
    }

    public void sendOfferSDP(String clientName, String targetName, SessionDescription sdp) {
        Map<String, String> basic = generateSysParam();
        basic.put("from", clientName);
        basic.put("target", targetName);
        basic.put("type", "offer");
        basic.put("sdp", sdp.description);

        JSONObject object = new JSONObject(basic);
        send(object.toString());

    }

    public void sendAnswerSDP(String targetName, SessionDescription sdp) {
        Map<String, String> basic = generateSysParam();
        basic.put("target", targetName);
        basic.put("type", "answer");
        basic.put("sdp", sdp.description);

        JSONObject object = new JSONObject(basic);
        send(object.toString());

    }

    public void sendIceCandidate(String targetName, IceCandidate candidate) {
        Map<String, String> basic = generateSysParam();
        basic.put("type", "icecandidate");
        basic.put("target", targetName);
        basic.put("id", candidate.sdpMid);
        basic.put("index", candidate.sdpMLineIndex+"");
        basic.put("sdp", candidate.sdp);

        JSONObject object = new JSONObject(basic);
        send(object.toString());
    }

    public void sendIceCandidatesRemove(String targetName, IceCandidate[] candidates) {
        Map<String, String> basic = generateSysParam();
        JSONObject object = new JSONObject();
        try {
            for (String key : basic.keySet()) {
                object.put(key, basic.get(key));
            }
            object.put("type", "removecandidate");
            object.put("target", targetName);

            JSONArray array = new JSONArray();
            for (int i = 0; i < candidates.length; ++i) {
                array.put(toJsonCandidate(candidates[i]));
            }
            object.put("array", array);
            send(object.toString());

        } catch (Exception e) {

        }
    }

    private JSONObject toJsonCandidate(IceCandidate candidate) {
        JSONObject data = new JSONObject();
        try {
            data.put("id", candidate.sdpMid);
            data.put("index", candidate.sdpMLineIndex);
            data.put("sdp", candidate.sdp);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return data;
    }
}
