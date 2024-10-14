package com.im.caller.rtc;


import android.content.Context;
import android.media.AudioManager;
import android.util.Log;
import androidx.annotation.Nullable;
import com.im.caller.bean.Role;
import com.im.caller.media.MediaCallback;
import com.im.caller.bean.Server;
import com.im.caller.ws.WsService;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.DataChannel;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.RtpTransceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoDecoderFactory;
import org.webrtc.VideoEncoderFactory;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import org.webrtc.audio.JavaAudioDeviceModule;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *   @desc : 用于处理 WebRTC 中的点对点连接
 *   @auth : tyf
 *   @date : 2024-10-08 15:07:04
*/
public class PeerService {

    public final static String TAG = PeerService.class.getName();

    // 视频分辨率、帧率、编码器配置
    public static final int VIDEO_RESOLUTION_WIDTH = 320;
    public static final int VIDEO_RESOLUTION_HEIGHT = 240;
    public static final int FPS = 10;
    public static final String VIDEO_CODEC_H264 = "H264";
    public static final String VIDEO_TRACK_ID = "ARDAMSv0";
    public static final String AUDIO_TRACK_ID = "ARDAMSa0";

    // webrtc 接口，用于创建 PeerConnection
    public PeerConnectionFactory peerFactory;
    // 本地流媒体
    public MediaStream localStream;
    // 本地视频轨道
    public VideoTrack localVideoTrack;
    // 本地音频轨道
    public AudioTrack localAudioTrack;
    // 本地视频捕获器
    public VideoCapturer captureAndroid;
    // 本地视频源
    public VideoSource videoSource;
    // 本地音频源
    public AudioSource audioSource;

    // 已经创建了链接的对端集合 id
    public ArrayList<String> connectionIdArray;
    public Map<String, Peer> connectionPeerDic;

    // 当前自身的 id
    public String myId;

    // 回调接口，在本地流准备好、对端流达到、流关闭时的回调函数
    public MediaCallback viewCallback;

    // 服务端地址列表集合
    public ArrayList<PeerConnection.IceServer> ICEServers;

    // 是否开启视频
    public boolean videoEnable;

    // 当前本端模式：1v1音频、1v1音视频、nvn群聊
    public int mediaType;

    private AudioManager mAudioManager;

    // 呼叫者 或者 被呼叫者
    private Role role;

    // JavaWebSocket（ws客户端）
    private WsService webSocket;

    private Context context;

    // 用于 OpenGL ES 的图形渲染
    private EglBase eglBase;

    // 视频捕获和渲染相关的工具
    @Nullable
    private SurfaceTextureHelper surfaceTextureHelper;

    // 单线程处理
    private final ExecutorService executor;

    public PeerService(WsService webSocket, Server[] iceServers) {
        this.connectionPeerDic = new HashMap<>();
        this.connectionIdArray = new ArrayList<>();
        this.ICEServers = new ArrayList<>();
        this.webSocket = webSocket;
        executor = Executors.newSingleThreadExecutor();
        if (iceServers != null) {
            // 初始化 ice（turn、stun） 服务器列表
            for (Server server : iceServers) {
                PeerConnection.IceServer iceServer = PeerConnection.IceServer
                        .builder(server.uri)
                        .setUsername(server.username)
                        .setPassword(server.password)
                        .createIceServer();
                ICEServers.add(iceServer);
            }
        }
    }

    // 设置界面的回调
    public void setViewCallback(MediaCallback callback) {
        viewCallback = callback;
    }


    // 设置和创建 WebRTC 的 PeerConnectionFactory
    private PeerConnectionFactory createConnectionFactory() {
        PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(context).createInitializationOptions());
        // 视频编解码器工厂
        final VideoEncoderFactory encoderFactory;
        final VideoDecoderFactory decoderFactory;
        encoderFactory = new DefaultVideoEncoderFactory(eglBase.getEglBaseContext(), true, true);
        decoderFactory = new DefaultVideoDecoderFactory(eglBase.getEglBaseContext());
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        return PeerConnectionFactory.builder()
                .setOptions(options)
                .setAudioDeviceModule(JavaAudioDeviceModule.builder(context).createAudioDeviceModule())
                .setVideoEncoderFactory(encoderFactory)
                .setVideoDecoderFactory(decoderFactory)
                .createPeerConnectionFactory();
    }

    // 创建本地流 MediaStream 对象（创建音频源音频轨道、视频源视频轨道，将音频轨道和视频轨道添加到本地流媒体中）
    private void createLocalStream() {
        // 创建一个本地媒体流，命名为 "ARDAMS
        localStream = peerFactory.createLocalMediaStream("ARDAMS");
        // 创建音频源、音频轨道
        audioSource = peerFactory.createAudioSource(createAudioConstraints());
        localAudioTrack = peerFactory.createAudioTrack(AUDIO_TRACK_ID, audioSource);
        // 音频加入本地流媒体
        localStream.addTrack(localAudioTrack);
        // 如果开启了视频
        if (videoEnable) {
            // 创建视频捕获器
            captureAndroid = createVideoCapture();
            // 视频源
            surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.getEglBaseContext());
            videoSource = peerFactory.createVideoSource(captureAndroid.isScreencast());
//            if (_mediaType == MediaType.TYPE_MEETING) {
                // videoSource.adaptOutputFormat(200, 200, 15);
//            }
            // 视频捕获器初始化并开始捕获视频
            captureAndroid.initialize(surfaceTextureHelper, context, videoSource.getCapturerObserver());
            captureAndroid.startCapture(VIDEO_RESOLUTION_WIDTH, VIDEO_RESOLUTION_HEIGHT, FPS);
            // 创建视频轨道
            localVideoTrack = peerFactory.createVideoTrack(VIDEO_TRACK_ID, videoSource);
            // 视频添加到本地流媒体中
            localStream.addTrack(localVideoTrack);
        }
        // 如果设置了回调函数则进行回调，通知外部本地流媒体已经打开准备好了
        if (viewCallback != null) {
            viewCallback.onSetLocalStream(localStream, myId);
        }
    }

    // 创建所有连接
    private void createPeerConnections() {
        for (Object str : connectionIdArray) {
            Peer peer = new Peer((String) str);
            connectionPeerDic.put((String) str, peer);
        }
    }

    // 为所有连接添加流
    private void addStreams() {
        for (Map.Entry<String, Peer> entry : connectionPeerDic.entrySet()) {
            if (localStream == null) {
                createLocalStream();
            }
            try {
                entry.getValue().pc.addStream(localStream);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    // 为所有连接创建offer
    private void createOffers() {
        for (Map.Entry<String, Peer> entry : connectionPeerDic.entrySet()) {
            role = Role.Caller;
            Peer mPeer = entry.getValue();
            mPeer.pc.createOffer(mPeer, offerOrAnswerConstraint());
        }

    }

    // 关闭通道流
    private void closePeerConnection(String connectionId) {
        Peer mPeer = connectionPeerDic.get(connectionId);
        if (mPeer != null) {
            mPeer.pc.close();
        }
        connectionPeerDic.remove(connectionId);
        connectionIdArray.remove(connectionId);
        if (viewCallback != null) {
            viewCallback.onCloseWithId(connectionId);
        }

    }

    // ===================================webSocket回调信息=======================================
    // 本端进入房间时的处理
    public void initContext(Context context, EglBase eglBase) {
        this.context = context;
        this.eglBase = eglBase;
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    // 本端进入房间成功（平台返回进入成功的消息）的处理
    public void onJoinToRoom(ArrayList<String> connections, String myId, boolean isVideoEnable, int mediaType) {
        videoEnable = isVideoEnable;
        this.mediaType = mediaType;
        executor.execute(() -> {
            connectionIdArray.addAll(connections);
            this.myId = myId;
            if (peerFactory == null) {
                peerFactory = createConnectionFactory();
            }
            if (localStream == null) {
                createLocalStream();
            }
            createPeerConnections();
            addStreams();
            createOffers();
        });
    }

    // 处理别人进入房间成功的消息
    public void onRemoteJoinToRoom(String socketId) {
        executor.execute(() -> {
            if (localStream == null) {
                createLocalStream();
            }
            try {
                Peer mPeer = new Peer(socketId);
                mPeer.pc.addStream(localStream);
                connectionIdArray.add(socketId);
                connectionPeerDic.put(socketId, mPeer);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

    }

    // 处理对端发送过来的 ICE 信息
    public void onRemoteIceCandidate(String socketId, IceCandidate iceCandidate) {
        executor.execute(() -> {
            Peer peer = connectionPeerDic.get(socketId);
            if (peer != null) {
                peer.pc.addIceCandidate(iceCandidate);
            }
        });
    }

    // 处理对端发送过来的 ICE 移除信息
    public void onRemoteIceCandidateRemove(String socketId, List<IceCandidate> iceCandidates) {
        // todo 移除
        executor.execute(() -> Log.d(TAG, "send onRemoteIceCandidateRemove"));
    }

    // 处理对端离开房间的消息
    public void onRemoteOutRoom(String socketId) {
        executor.execute(() -> closePeerConnection(socketId));
    }

    // 处理接收到对端的 SDP（Session Description Protocol）信息
    public void onReceiveOffer(String socketId, String description) {
        executor.execute(() -> {
            role = Role.Receiver;
            Peer mPeer = connectionPeerDic.get(socketId);
            String sessionDescription = description;
            if (videoEnable) {
                sessionDescription = preferCodec(description, VIDEO_CODEC_H264, false);
            }
            SessionDescription sdp = new SessionDescription(SessionDescription.Type.OFFER, sessionDescription);
            if (mPeer != null) {
                mPeer.pc.setRemoteDescription(mPeer, sdp);
            }
        });

    }

    // 处理接收到对端的的 SDP（Session Description Protocol） 回复信息
    public void onReceiverAnswer(String socketId, String sdp) {
        executor.execute(() -> {
            Peer mPeer = connectionPeerDic.get(socketId);
            SessionDescription sessionDescription = new SessionDescription(SessionDescription.Type.ANSWER, sdp);
            if (mPeer != null) {
                mPeer.pc.setRemoteDescription(mPeer, sessionDescription);
            }
        });
    }

    //**************************************逻辑控制**************************************

    // 调整摄像头前置后置
    public void switchCamera() {
        if (captureAndroid == null) return;
        if (captureAndroid instanceof CameraVideoCapturer) {
            CameraVideoCapturer cameraVideoCapturer = (CameraVideoCapturer) captureAndroid;
            cameraVideoCapturer.switchCamera(null);
        } else {
            Log.d(TAG, "Will not switch camera, video caputurer is not a camera");
        }
    }

    // 设置自己静音
    public void toggleMute(boolean enable) {
        if (localAudioTrack != null) {
            localAudioTrack.setEnabled(enable);
        }
    }

    // 设置为免提
    public void toggleSpeaker(boolean enable) {
        if (mAudioManager != null) {
            mAudioManager.setSpeakerphoneOn(enable);
        }
    }

    // 退出房间
    public void exitRoom() {
        if (viewCallback != null) {
            viewCallback = null;
        }
        executor.execute(() -> {
            ArrayList myCopy;
            myCopy = (ArrayList) connectionIdArray.clone();
            for (Object Id : myCopy) {
                closePeerConnection((String) Id);
            }
            if (connectionIdArray != null) {
                connectionIdArray.clear();
            }
            if (audioSource != null) {
                audioSource.dispose();
                audioSource = null;
            }
            if (videoSource != null) {
                videoSource.dispose();
                videoSource = null;
            }
            if (captureAndroid != null) {
                try {
                    captureAndroid.stopCapture();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                captureAndroid.dispose();
                captureAndroid = null;
            }
            if (surfaceTextureHelper != null) {
                surfaceTextureHelper.dispose();
                surfaceTextureHelper = null;
            }
            if (peerFactory != null) {
                peerFactory.dispose();
                peerFactory = null;
            }
            if (webSocket != null) {
                webSocket.close();
                webSocket = null;
            }
        });
    }


    private VideoCapturer createVideoCapture() {
        VideoCapturer videoCapturer;
        if (useCamera2()) {
            videoCapturer = createCameraCapture(new Camera2Enumerator(context));
        } else {
            videoCapturer = createCameraCapture(new Camera1Enumerator(true));
        }
        return videoCapturer;
    }

    private VideoCapturer createCameraCapture(CameraEnumerator enumerator) {
        final String[] deviceNames = enumerator.getDeviceNames();
        // First, try to find front facing camera
        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);
                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }
        // Front facing camera not found, try something else
        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }
        return null;
    }

    private boolean useCamera2() {
        return Camera2Enumerator.isSupported(context);
    }


    //**************************************各种约束******************************************/

    private static final String AUDIO_ECHO_CANCELLATION_CONSTRAINT = "googEchoCancellation";
    private static final String AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT = "googAutoGainControl";
    private static final String AUDIO_HIGH_PASS_FILTER_CONSTRAINT = "googHighpassFilter";
    private static final String AUDIO_NOISE_SUPPRESSION_CONSTRAINT = "googNoiseSuppression";

    private MediaConstraints createAudioConstraints() {
        Log.d(TAG,"PeerConnectionHelper.createAudioConstraints");
        MediaConstraints audioConstraints = new MediaConstraints();
        audioConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair(AUDIO_ECHO_CANCELLATION_CONSTRAINT, "true"));
        audioConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair(AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT, "false"));
        audioConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair(AUDIO_HIGH_PASS_FILTER_CONSTRAINT, "true"));
        audioConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair(AUDIO_NOISE_SUPPRESSION_CONSTRAINT, "true"));
        return audioConstraints;
    }

    private MediaConstraints offerOrAnswerConstraint() {
        Log.d(TAG,"PeerConnectionHelper.offerOrAnswerConstraint");
        MediaConstraints mediaConstraints = new MediaConstraints();
        ArrayList<MediaConstraints.KeyValuePair> keyValuePairs = new ArrayList<>();
        keyValuePairs.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        keyValuePairs.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", String.valueOf(videoEnable)));
        mediaConstraints.mandatory.addAll(keyValuePairs);
        return mediaConstraints;
    }

    //**************************************内部类******************************************/
    private class Peer implements SdpObserver, PeerConnection.Observer {
        private PeerConnection pc;
        private String socketId;

        public Peer(String socketId) {
            this.pc = createPeerConnection();
            this.socketId = socketId;

        }


        //****************************PeerConnection.Observer****************************/

        @Override
        public void onSignalingChange(PeerConnection.SignalingState signalingState) {
            Log.i(TAG, "onSignalingChange: " + signalingState);
        }

        @Override
        public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
            Log.i(TAG, "onIceConnectionChange: " + iceConnectionState.toString());
        }

        @Override
        public void onConnectionChange(PeerConnection.PeerConnectionState newState) {
            Log.i(TAG, "onConnectionChange: " + newState.toString());
        }

        @Override
        public void onIceConnectionReceivingChange(boolean b) {
            Log.i(TAG, "onIceConnectionReceivingChange:" + b);

        }

        @Override
        public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
            Log.i(TAG, "onIceGatheringChange:" + iceGatheringState.toString());

        }

        @Override
        public void onIceCandidate(IceCandidate iceCandidate) {
            Log.d(TAG,"PeerConnectionHelper.onIceCandidate");
            // 发送IceCandidate
            webSocket.sendIceCandidate(socketId, iceCandidate);
        }

        @Override
        public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
            Log.i(TAG, "onIceCandidatesRemoved:");
        }

        @Override
        public void onAddStream(MediaStream mediaStream) {
            Log.d(TAG,"PeerConnectionHelper.onAddStream");
            if (viewCallback != null) {
                viewCallback.onAddRemoteStream(mediaStream, socketId);
            }
        }

        @Override
        public void onRemoveStream(MediaStream mediaStream) {
            Log.d(TAG,"PeerConnectionHelper.onRemoveStream");
            if (viewCallback != null) {
                viewCallback.onCloseWithId(socketId);
            }
        }

        @Override
        public void onDataChannel(DataChannel dataChannel) {
            Log.d(TAG,"PeerConnectionHelper.onDataChannel");
        }

        @Override
        public void onRenegotiationNeeded() {
            Log.d(TAG,"PeerConnectionHelper.onRenegotiationNeeded");
        }

        @Override
        public void onAddTrack(RtpReceiver receiver, MediaStream[] mediaStreams) {
            Log.d(TAG,"PeerConnectionHelper.onAddTrack");
        }

        @Override
        public void onTrack(RtpTransceiver transceiver) {
            Log.d(TAG,"PeerConnectionHelper.onTrack");
        }

        //****************************SdpObserver****************************/
        @Override
        public void onCreateSuccess(SessionDescription origSdp) {
            //设置本地的SDP
            String sdpDescription = origSdp.description;
            if (videoEnable) {
                sdpDescription = preferCodec(sdpDescription, VIDEO_CODEC_H264, false);
            }
            final SessionDescription sdp = new SessionDescription(origSdp.type, sdpDescription);
            pc.setLocalDescription(Peer.this, sdp);
        }

        @Override
        public void onSetSuccess() {
            if (pc.signalingState() == PeerConnection.SignalingState.HAVE_REMOTE_OFFER) {
                pc.createAnswer(Peer.this, offerOrAnswerConstraint());
            } else if (pc.signalingState() == PeerConnection.SignalingState.HAVE_LOCAL_OFFER) {
                //判断连接状态为本地发送offer
                if (role == Role.Receiver) {
                    //接收者，发送Answer
                    webSocket.sendAnswer(socketId, pc.getLocalDescription().description);

                } else if (role == Role.Caller) {
                    //发送者,发送自己的offer
                    webSocket.sendOffer(socketId, pc.getLocalDescription().description);
                }
            } else if (pc.signalingState() == PeerConnection.SignalingState.STABLE) {
                // Stable 稳定的
                if (role == Role.Receiver) {
                    webSocket.sendAnswer(socketId, pc.getLocalDescription().description);

                }
            }
        }
        @Override
        public void onCreateFailure(String s) {

        }
        @Override
        public void onSetFailure(String s) {

        }
        //初始化 RTCPeerConnection 连接管道
        private PeerConnection createPeerConnection() {
            if (peerFactory == null) {
                peerFactory = createConnectionFactory();
            }
            // 管道连接抽象类实现方法
            PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(ICEServers);
            return peerFactory.createPeerConnection(rtcConfig, this);
        }
    }


    // ===================================替换编码方式优先级========================================

    private static String preferCodec(String sdpDescription, String codec, boolean isAudio) {
        final String[] lines = sdpDescription.split("\r\n");
        final int mLineIndex = findMediaDescriptionLine(isAudio, lines);
        if (mLineIndex == -1) {
            Log.w(TAG, "No mediaDescription line, so can't prefer " + codec);
            return sdpDescription;
        }
        // A list with all the payload types with name |codec|. The payload types are integers in the
        // range 96-127, but they are stored as strings here.
        final List<String> codecPayloadTypes = new ArrayList<>();
        // a=rtpmap:<payload type> <encoding name>/<clock rate> [/<encoding parameters>]
        final Pattern codecPattern = Pattern.compile("^a=rtpmap:(\\d+) " + codec + "(/\\d+)+[\r]?$");
        for (String line : lines) {
            Matcher codecMatcher = codecPattern.matcher(line);
            if (codecMatcher.matches()) {
                codecPayloadTypes.add(codecMatcher.group(1));
            }
        }
        if (codecPayloadTypes.isEmpty()) {
            Log.w(TAG, "No payload types with name " + codec);
            return sdpDescription;
        }

        final String newMLine = movePayloadTypesToFront(codecPayloadTypes, lines[mLineIndex]);
        if (newMLine == null) {
            return sdpDescription;
        }
        Log.d(TAG, "Change media description from: " + lines[mLineIndex] + " to " + newMLine);
        lines[mLineIndex] = newMLine;
        return joinString(Arrays.asList(lines), "\r\n", true /* delimiterAtEnd */);
    }

    private static int findMediaDescriptionLine(boolean isAudio, String[] sdpLines) {
        final String mediaDescription = isAudio ? "m=audio " : "m=video ";
        for (int i = 0; i < sdpLines.length; ++i) {
            if (sdpLines[i].startsWith(mediaDescription)) {
                return i;
            }
        }
        return -1;
    }

    private static @Nullable
    String movePayloadTypesToFront(List<String> preferredPayloadTypes, String mLine) {
        // The format of the media description line should be: m=<media> <port> <proto> <fmt> ...
        final List<String> origLineParts = Arrays.asList(mLine.split(" "));
        if (origLineParts.size() <= 3) {
            Log.e(TAG, "Wrong SDP media description format: " + mLine);
            return null;
        }
        final List<String> header = origLineParts.subList(0, 3);
        final List<String> unpreferredPayloadTypes =
                new ArrayList<>(origLineParts.subList(3, origLineParts.size()));
        unpreferredPayloadTypes.removeAll(preferredPayloadTypes);
        // Reconstruct the line with |preferredPayloadTypes| moved to the beginning of the payload
        // types.
        final List<String> newLineParts = new ArrayList<>();
        newLineParts.addAll(header);
        newLineParts.addAll(preferredPayloadTypes);
        newLineParts.addAll(unpreferredPayloadTypes);
        return joinString(newLineParts, " ", false /* delimiterAtEnd */);
    }

    private static String joinString(Iterable<? extends CharSequence> s, String delimiter, boolean delimiterAtEnd) {
        Iterator<? extends CharSequence> iter = s.iterator();
        if (!iter.hasNext()) {
            return "";
        }
        StringBuilder buffer = new StringBuilder(iter.next());
        while (iter.hasNext()) {
            buffer.append(delimiter).append(iter.next());
        }
        if (delimiterAtEnd) {
            buffer.append(delimiter);
        }
        return buffer.toString();
    }

}



