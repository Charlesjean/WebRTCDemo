package com.example.duanjin.webrtcdemo;

import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import java.util.List;

/**
 * Created by duanjin on 6/11/17.
 */

public interface IPeerStateListener {

    /**
     * 收到server端连接建立后的消息
     */
    void onReceiveClientId(String id);
    void onReceiveUsers(List<String> users);
    void onReceivePeerOffer(String peerName, SessionDescription sdp);
    void onReceivePeerAnswer(SessionDescription sdp);
    void onReceiveIceCandidate(IceCandidate candidate);
    void onReceiveIceCandidatesRemove(IceCandidate[] candidates);

}
