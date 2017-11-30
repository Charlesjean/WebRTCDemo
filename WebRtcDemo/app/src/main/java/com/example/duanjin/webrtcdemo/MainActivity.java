package com.example.duanjin.webrtcdemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import org.webrtc.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements IPeerStateListener{

    private static final String TAG = MainActivity.class.getName();

    private SurfaceViewRenderer mSurfaceViewRender;
    private SurfaceViewRenderer mRemoteViewRender;
    private View mBtn;
    private WebSocketChannel mSocket;
    private String mClientName;
    private TextView mTargetTxt;

    private String mTargetUser;
    private PeerConnection mPeerConnection;

    private VideoTrack mRemoteVideoTrack;

    private List<PeerConnection.IceServer> mIceServers = new ArrayList<>();


    private EglBase mGLBase;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBtn = findViewById(R.id.start_btn);
        mSurfaceViewRender = (SurfaceViewRenderer) findViewById(R.id.render_view);
        mGLBase = EglBase.create();
        mSurfaceViewRender.init(mGLBase.getEglBaseContext(), null);

        mRemoteViewRender = (SurfaceViewRenderer) findViewById(R.id.remote_render_view);
        mRemoteViewRender.init(mGLBase.getEglBaseContext(), null);

        mBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mTargetUser = mTargetTxt.getText().toString();
                startChat();
            }
        });

        mTargetTxt = (TextView) findViewById(R.id.target);
        mTargetTxt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mTargetUser = mTargetTxt.getText().toString();
            }
        });

        try {
            URI uri = new URI("ws://30.117.84.41:1234");
            mSocket = new WebSocketChannel(uri);
            mSocket.setPeerStateListener(this);
            mSocket.connect();

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        mIceServers.add(new PeerConnection.IceServer("stun:stun.services.mozilla.com"));
        mIceServers.add(new PeerConnection.IceServer("stun:stun.l.google.com:19302"));
        initLocalParam();
    }

    private void initLocalParam() {

        PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(getApplicationContext())
                                                                                    .createInitializationOptions());
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        PeerConnectionFactory peerConnectionFactory = new PeerConnectionFactory(options);
        peerConnectionFactory.setVideoHwAccelerationOptions(mGLBase.getEglBaseContext(), mGLBase.getEglBaseContext());

        VideoCapturer videoCapturer = initVideoCapture();
        VideoSource videoSource = peerConnectionFactory.createVideoSource(videoCapturer);
        VideoTrack videoTrack = peerConnectionFactory.createVideoTrack("local", videoSource);

        videoCapturer.startCapture(500, 500, 30);
        videoTrack.addRenderer(new VideoRenderer(mSurfaceViewRender));

        //TODO  need configuration
        MediaConstraints mediaConstraints = new MediaConstraints();
        mPeerConnection = peerConnectionFactory.createPeerConnection(mIceServers,
                mediaConstraints, mObserver);

        MediaStream stream = peerConnectionFactory.createLocalMediaStream("100");
        stream.addTrack(videoTrack);
        mPeerConnection.addStream(stream);
    }
    private void startChat() {

        mPeerConnection.createOffer(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                mPeerConnection.setLocalDescription(new SdpObserver() {
                    @Override
                    public void onCreateSuccess(SessionDescription sessionDescription) {

                    }

                    @Override
                    public void onSetSuccess() {

                    }

                    @Override
                    public void onCreateFailure(String s) {

                    }

                    @Override
                    public void onSetFailure(String s) {

                    }
                }, sessionDescription);

                Log.i(TAG, "SDP create success: " + sessionDescription.description);
                //TODO send sdp to server and to remote peer
                sendOfferSDP(sessionDescription);

            }

            @Override
            public void onSetSuccess() {

            }

            @Override
            public void onCreateFailure(String s) {
                Log.i(TAG, "SDP create fail: " + s);
            }

            @Override
            public void onSetFailure(String s) {

            }
        }, new MediaConstraints());
    }

    private VideoCapturer initVideoCapture() {

        CameraEnumerator enumerator = new Camera2Enumerator(getApplicationContext());
        String[] cameras = enumerator.getDeviceNames();
        for (int i = 0; i < cameras.length; ++i ) {
            if (enumerator.isFrontFacing(cameras[i])) {
                VideoCapturer capturer = enumerator.createCapturer(cameras[i], null);
                if (capturer != null) {
                    return capturer;
                }
            }
        }

        for (int i = 0; i < cameras.length; ++i) {
            VideoCapturer capturer = enumerator.createCapturer(cameras[i], null);
            if (capturer != null) {
                return capturer;
            }
        }

        return null;
    }

    PeerConnection.Observer mObserver = new PeerConnection.Observer() {
        @Override
        public void onSignalingChange(PeerConnection.SignalingState signalingState) {
            Log.i(TAG, "onSignalingChange");

        }

        @Override
        public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
            Log.i(TAG, "onIceConnectionChange");
        }

        @Override
        public void onIceConnectionReceivingChange(boolean b) {
            Log.i(TAG, "onIceConnectionReceivingChange");
        }

        @Override
        public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
            Log.i(TAG, "onIceGatheringChange");
        }

        @Override
        public void onIceCandidate(IceCandidate iceCandidate) {
            //TODO send candidate to remote peer
            Log.i(TAG, "onIceCandidate");
            sendIceCandidate(iceCandidate);

        }

        @Override
        public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
            Log.i(TAG, "onIceCandidatesRemoved");
            sendIceCandidatesRemove(iceCandidates);
        }

        @Override
        public void onAddStream(MediaStream mediaStream) {

            //TODO remote stream added
            Log.i(TAG, "onAddStream");
            if (mediaStream.videoTracks.size() == 1) {
                mRemoteVideoTrack = mediaStream.videoTracks.get(0);
                mRemoteVideoTrack.setEnabled(true);
                mRemoteVideoTrack.addRenderer(new VideoRenderer(mRemoteViewRender));
            }

        }

        @Override
        public void onRemoveStream(MediaStream mediaStream) {
            Log.i(TAG, "onRemoveStream");
        }

        @Override
        public void onDataChannel(DataChannel dataChannel) {
            Log.i(TAG, "onDataChannel");
        }

        @Override
        public void onRenegotiationNeeded() {
            Log.i(TAG, "onRenegotiationNeeded");
        }

        @Override
        public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
            Log.i(TAG, "onAddTrack");
        }
    };

    /**
     * PeerStateListener 实现
     */
    @Override
    public void onReceiveClientId(String id) {
        Log.i(TAG, "receive from server: cliendId is " + id);
        Map<String, String> datas = new HashMap<>();
        datas.put("type", "register");

        mClientName = "1234" + Math.random();
        datas.put("name", mClientName);
        mSocket.send(datas);
    }

    @Override
    public void onReceiveUsers(List<String> users) {

        Log.i(TAG, "receive from server new user");
        if (users != null && users.size() > 0) {
            users.remove(mClientName);
        }

        if (users.size() > 0) {
            mTargetTxt.setText(users.get(0));
            mTargetTxt.invalidate();
        }
    }

    @Override
    public void onReceivePeerOffer(String peerName, SessionDescription sdp) {
        Log.i(TAG, "onReceivePeerOffer " + sdp.description);
        mTargetUser = peerName;
        mPeerConnection.setRemoteDescription(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {

            }

            @Override
            public void onSetSuccess() {

            }

            @Override
            public void onCreateFailure(String s) {

            }

            @Override
            public void onSetFailure(String s) {

            }
        }, sdp);
        mPeerConnection.createAnswer(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                mPeerConnection.setLocalDescription(new SdpObserver() {
                    @Override
                    public void onCreateSuccess(SessionDescription sessionDescription) {

                    }

                    @Override
                    public void onSetSuccess() {

                    }

                    @Override
                    public void onCreateFailure(String s) {

                    }

                    @Override
                    public void onSetFailure(String s) {

                    }
                }, sessionDescription);
                //TODO send answer
                sendAnswerSDP(sessionDescription);

            }

            @Override
            public void onSetSuccess() {

            }

            @Override
            public void onCreateFailure(String s) {

            }

            @Override
            public void onSetFailure(String s) {

            }
        }, new MediaConstraints());
    }

    @Override
    public void onReceivePeerAnswer(SessionDescription sdp) {
        Log.i(TAG, "onReceivePeerAnswer: " + sdp.description);
        mPeerConnection.setRemoteDescription(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {

            }

            @Override
            public void onSetSuccess() {

            }

            @Override
            public void onCreateFailure(String s) {

            }

            @Override
            public void onSetFailure(String s) {

            }
        }, sdp);

    }

    @Override
    public void onReceiveIceCandidate(IceCandidate candidate) {
        Log.i(TAG, "onReceiveIceCandidate " + candidate.sdp + " " + candidate.sdpMid + " " + candidate.sdp);
        mPeerConnection.addIceCandidate(candidate);
    }

    @Override
    public void onReceiveIceCandidatesRemove(IceCandidate[] candidates) {
        Log.i(TAG, "onReceiveIceCandidatesRemove");
        mPeerConnection.removeIceCandidates(candidates);
    }

    private void sendOfferSDP(SessionDescription sdp) {
        mSocket.sendOfferSDP(mClientName, mTargetUser, sdp);
    }

    private void sendAnswerSDP(SessionDescription sdp) {
        mSocket.sendAnswerSDP(mTargetUser, sdp);
    }

    private void sendIceCandidate(IceCandidate candidate) {
        mSocket.sendIceCandidate(mTargetUser, candidate);
    }

    private void sendIceCandidatesRemove(IceCandidate[] candidates) {
        mSocket.sendIceCandidatesRemove(mTargetUser, candidates);
    }
}
