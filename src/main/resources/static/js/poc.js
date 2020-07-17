//connecting to our signaling server 
var host = window.location.hostname
var port = window.location.port

const mediaStreamConstraints = {
    video: true, audio: true
};

// Video element where stream will be placed.
const localVideo = document.getElementById('local-view');
const remoteVideo = document.getElementById('remote-view');

let localStream;
let remoteStream;

// Handles success by adding the MediaStream to the video element.
function gotLocalMediaStream(mediaStream) {
    localStream = mediaStream;
    localVideo.srcObject = mediaStream;
}

// Handles error by logging a message to the console with the error message.
function handleLocalMediaStreamError(error) {
    console.log('navigator.getUserMedia error: ', error);
}

// Initializes media stream.
navigator.mediaDevices.getUserMedia(mediaStreamConstraints)
        .then(gotLocalMediaStream).catch(handleLocalMediaStreamError);



console.log("host/port=>", host, port)
var conn = new WebSocket('wss://' + host + ':' + port + '/socket');

conn.onopen = function () {
    console.log("Connected to the signaling server");
    //initialize();
};

conn.onmessage = function (msg) {
    console.log("Got message", msg.data);
    var content = JSON.parse(msg.data);
    var data = content.data;
    switch (content.event) {
        // when somebody wants to call us
        case "offer":
            handleOffer(data);
            break;
        case "answer":
            handleAnswer(data);
            break;
            // when a remote peer sends an ice candidate to us
        case "candidate":
            handleCandidate(data);
            break;
        default:
            break;
    }
};

function send(message) {
    conn.send(JSON.stringify(message));
}

var peerConnection;

var dataChannel;
var input = document.getElementById("messageInput");

function initialize() {
    console.log("initializing")
    var uuid = uuidv4();
    //var configuration = null
    var configuration = {
        'iceServers': [{
                'url': 'stun:stun.stunprotocol.org:3478'
            }]
    };

    peerConnection = new RTCPeerConnection(configuration, {
        optional: [{
                RtpDataChannels: true
           }]
     });
   // peerConnection = new RTCPeerConnection(configuration);
    // var tracks = localStream.getTracks();
    //for (var i = 0; i < tracks.length; i++) {
    //    peerConnection.addTrack(tracks[i]);
    //    console.log("adding track", tracks[i])
    //}
    localStream.getTracks().forEach(function(track) {
        console.log("add track",track)
        console.log("toStream",localStream)
    peerConnection.addTrack(track, localStream);
  });
    
    // Setup ice handling
    peerConnection.onicecandidate = function (event) {
        
        if (event.candidate) {
            send({
                event: "candidate",
                data: event.candidate
            });
        }
    };
    // video
 
   // peerConnection.ontrack = function (event) {
   peerConnection.ontrack = function(event) {
        console.log("on track event", event)
        //remoteVideo.srcObject = event.stream;

        console.log("adding video stream to remoteVideo target pre",remoteVideo)
        remoteVideo.srcObject = event.streams[0];
         console.log("adding video stream to remoteVideo target post",remoteVideo)

    };   




    // creating data channel
    dataChannel = peerConnection.createDataChannel("dataChannel", {
        reliable: true
    });

    dataChannel.onerror = function (error) {
        console.log("Error occured on datachannel:", error);
    };

    // when we receive a message from the other peer, printing it on the console
    dataChannel.onmessage = function (event) {
        console.log("message:", event.data);
    };

    dataChannel.onclose = function () {
        console.log("data channel is closed");
    };
}

function createOffer() {
   

    peerConnection.createOffer(function (offer) {
        send({
            event: "offer",
            data: offer
        });
        peerConnection.setLocalDescription(offer);
    }, function (error) {
        alert("Error creating an offer");
    });
}

function handleOffer(offer) {
    console.log("handleOffer",offer)
    peerConnection.setRemoteDescription(new RTCSessionDescription(offer));

    // create and send an answer to an offer
    peerConnection.createAnswer(function (answer) {
        peerConnection.setLocalDescription(answer);
        send({
            event: "answer",
            data: answer
        });
    }, function (error) {
        alert("Error creating an answer");
    });

}
;

function handleCandidate(candidate) {
    console.log("handleCandidate",candidate)
    peerConnection.addIceCandidate(new RTCIceCandidate(candidate));
}
;

function handleAnswer(answer) {
    console.log("handleAnswer",answer)
    peerConnection.setRemoteDescription(new RTCSessionDescription(answer));

    console.log("connection established successfully!!");
}
;

function sendMessage() {
    dataChannel.send(input.value);
    input.value = "";
}

function uuidv4() {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
        var r = Math.random() * 16 | 0, v = c == 'x' ? r : (r & 0x3 | 0x8);
        return v.toString(16);
    });
}