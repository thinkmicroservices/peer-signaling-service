console.log(!!navigator.mediaDevices.getSupportedConstraints().mediaSource);
//constraints.video.mandatory.chromeMediaSource = 'screen'
//connecting to our signaling server 
var host = window.location.hostname;
var port = window.location.port;
var path = "/api/webrtc/";
var ringBackTone= path+'audio/creative-commons-us-ringback-tone.ogg';
var ringTone='audio/creative-commons-ring-tone.ogg';

localVideoCallerId = uuidv4();

remoteVideoCallerId = null;


const mediaStreamConstraints = {
    video: true,

    audio: true,
    mediaSource: 'screen'
};

// Video element where stream will be placed.
const localVideo = document.getElementById('local-view');
const remoteVideo = document.getElementById('remote-view');

let localStream;
let remoteStream;

// Handles success by adding the MediaStream to the video element.
function obtainedLocalMediaStream(mediaStream) {
    localStream = mediaStream;
    localVideo.srcObject = mediaStream;
    localVideo.muted = true;


    createSignalingConnection().then(function (signalingConnection) {
        initializeRTCPeer();
    }).catch(function (err) {
        console.error("singnaling connection error:", error);
    });
}

// Handles error by logging a message to the console with the error message.
function handleLocalMediaStreamError(error) {
    console.log('navigator.getUserMedia error: ', error);

}

// Initializes media stream.
async function initializeLocalMediaStream() {

    navigator.mediaDevices.getUserMedia(mediaStreamConstraints)
            .then(obtainedLocalMediaStream).catch(handleLocalMediaStreamError);
}



var signalingConnection;

var currentOutboundCallVideoUserId;

async function createSignalingConnection() {


    return new Promise(function (resolve, reject) {

        // create connection
        signalingConnection = new WebSocket('wss://' + host + ':' + port + path+'conference');


        // connection has been opened
        signalingConnection.onopen = function () {
            console.log("Connected to the signaling server");
            // alert("connected to the signaling server" );
            resolve(signalingConnection);
        };

        // incoming message from signaling service
        signalingConnection.onmessage = function (msg) {
            console.log("Got message", msg.data);

            var content = JSON.parse(msg.data);
            var data = content.data;

            // inject the caller into the data 
            data["caller"] = content.caller;

            console.log("data from signaling", data)

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

                case "room-created":
                    $("#createMeetingRoomName").val(data);
                    break

                case "user-registered":
                    console.log("user registered");
                    break

                case "active-user-list":
                    console.log("rx active user list");
                    activeUserListUpdated(data);
                    break

                case "initiate-call":
                    console.log("incoming call request", msg);
                    showIncomingCallDialog(msg);
                    break


                default:
                    break;
            }
        };
        // an error occured
        signalingConnection.onerror = function (err) {
            console.error("signaling connection error:", err);
            alert("signaling connection error");
        };

        signalingConnection.onclose = function (event) {
            console.error("signaling connection is closed now.");
        };


        // reject(?)
    });

}
function send(message) {
    console.log("send", message);
    signalingConnection.send(JSON.stringify(message));
}

function sendEvent(type, destinationSessionId, event) {
    console.log("sendEvent", type, destinationSessionId, event);
    signalingConnection.send("event-" + type + "|" + destinationSessionId + "|" + JSON.stringify(event));
}

var peerConnection;

var dataChannel;
var input = document.getElementById("messageInput");

async function initializeRTCPeer() {
    console.log("initializing");

    var uuid = uuidv4();

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

    localStream.getTracks().forEach(function (track) {

        peerConnection.addTrack(track, localStream);
    });

    // Setup ice handling
    peerConnection.onicecandidate = function (event) {
        console.log("onicecandidate", event);
        if (event.candidate) {
            sendEvent("candidate", remoteVideoCallerId, {
                event: "candidate",
                data: event.candidate
            });
        }
    };
    // video


    peerConnection.ontrack = function (event) {

        console.log("on track event", event);
        if (event.track.kind === "video") {
            console.log("on track>video")



            createNewVideoElement(event);





            console.log("adding video stream to remoteVideo target post", newVideo);
        } else {
            console.log("...")
        }
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

function createNewVideoElement(event) {
    console.log("create new video element", event);
    var videoId = event.track.id;
    var newVideo = document.createElement("video");
    //newVideo.class = "remoteVideo";create new video
    newVideo.id = videoId;
    newVideo.autoplay = true;
    newVideo.playsinline = true;
    newVideo.console = true; //turn off console
    //newVideo.srcObject = event.streams[0];
    //remoteVideo.src = window.URL.createObjectURL(event.streams[0]);
    newVideo.srcObject = event.streams[0]



    $('.video-container').append(newVideo);
    $("#" + videoId).attr('controls', true);

    $("#" + videoId).bind("click", function () {
        var vid = $(this).get(0);
        var hangupFlag = confirm("hangup")
    });
}
function createOffer() {

    console.log("create offer");
    peerConnection.createOffer(function (offer) {
        console.log("send event....")
        sendEvent("offer", remoteVideoCallerId, {
            event: "offer",
            caller: localVideoCallerId,
            callee: remoteVideoCallerId,
            data: offer
        });
       console.log("set local description",offer);
        peerConnection.setLocalDescription(offer);
    }, function (error) {
        alert("Error creating an offer");
    });
}

function handleOffer(offer) {
    console.log("handleOffer", offer);


    peerConnection.setRemoteDescription(new RTCSessionDescription(offer));
    remoteVideoCallerId = offer.caller;


    var acceptCallFlag = window.confirm("accept call");
    if (acceptCallFlag) {
        // create and send an answer to an offer
        peerConnection.createAnswer(function (answer) {
            peerConnection.setLocalDescription(answer);
            console.log("send answer", remoteVideoCallerId);
            sendEvent("answer", remoteVideoCallerId, {
                event: "answer",

                data: answer
            });


        }, function (error) {
            alert("Error creating an answer");
        });
    } else {
        alert("call ignored");
    }

}
;

function handleCandidate(candidate) {
    console.log("handleCandidate", candidate);

    peerConnection.addIceCandidate(new RTCIceCandidate(candidate));
}
;

function handleAnswer(answer) {
    console.log("handleAnswer", answer);

    peerConnection.setRemoteDescription(new RTCSessionDescription(answer));
    pauseAudio(); // turn off ringer
    console.log("connection established successfully!!");
}
;

// send a message on the peer rtc data chanel
// TODO add an input channel
function sendMessage() {
    dataChannel.send(input.value);

    input.value = "";
}

// placeholder for the user id
function uuidv4() {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
        var r = Math.random() * 16 | 0, v = c == 'x' ? r : (r & 0x3 | 0x8);
        return v.toString(16);
    });
}











// create meeting room name


function generateNewMeetingRoomName() {
    console.log("generate meeting room name")
    sendEvent("create-room", localVideoCallerId, {})

}

function getActiveUsers() {
    console.log("get active users")
    sendEvent("get-active-users", localVideoCallerId, {});
}


 
// jquery handlers
$(document).ready(function () {

// meeting room
    $("#generateMeetingRoomButton").click(function () {
        console.log("generate meeting room click")
        generateNewMeetingRoomName()
    });

    $("#registerVideoUserButton").click(function () {
        console.log("register video user click")
        var event = {id: localVideoCallerId, displayName: $("#videoUserName").val()};
        console.log("connectToExchange", event)
        sendEvent("register-user", localVideoCallerId, event)
    });



    $("#outgoing-call-initiate-button").click(function () {
        console.log("outgoing call initiate click")
        initiateOutboundCall();
    });

    $("#incoming-call-answer-button").click(function () {
        console.log("incoming call answer click")
        pauseAudio()
    });

    $("#incoming-call-ignore-button").click(function () {
        console.log("incoming call ignore click")
        pauseAudio();
    });


    $('#active-user-list').change(function () {
        currentOutboundCallVideoUserId = $(this).val()[0];
        remoteVideoCallerId = currentOutboundCallVideoUserId
        console.log("selected video user id", currentOutboundCallVideoUserId);

        var selectedOptionLabel = $('#active-user-list option:selected').text()
        $("#outgoing-call-name").text(selectedOptionLabel);

        $("#outgoing-call-modal-dialog").modal("show");
    });

    $('.video').parent().click(function () {
        if ($(this).children(".video")) {
            // $(this).children(".video").get(0).play();
            $(this).children(".playpause").fadeOut();
            alert("click 1");
        } else {
            //$(this).children(".video").get(0).pause();
            $(this).children(".playpause").fadeIn();
            alert("click 2");
        }
    });


});

function initiateOutboundCall() {
    playAudio(ringBackTone);
    var initiateCallEvent = {
        event: "initiate-call",
        callerDisplayName: $("#videoUserName").val()
    };
    // sendEvent("initiate-call", remoteVideoCallerId, initiateCallEvent);
    createOffer();
}

function showIncomingCallDialog(connectionInfo) {
    playAudio(ringTone);
    $("#incoming-call-name").text(connectionInfo.callerDisplayName)
    $("#incoming-call-modal-dialog").modal("show");

}
function playAudio(audioFile) {
    document.getElementById("ringer-audio").setAttribute('src', audioFile);
    document.getElementById("ringer-audio").play();
    document.getElementById("ringer-audio").loop = true
    document.getElementById("ringer-audio").vol = 0.4;
}

function pauseAudio() {
    document.getElementById("ringer-audio").pause();
}




function activeUserListUpdated(userList) {
    console.log("activeUserListUpdated", userList)


    var newOptions = {}
    for (var idx = 0; idx < userList.length; idx++) {
        //console.log("add new user", userList[idx])
        newOptions[ (userList[idx].displayName) ] = userList[idx].id
    }
    //console.log("new options", newOptions)

    var $el = $("#active-user-list");
    $el.empty(); // remove old options
    $.each(newOptions, function (key, value) {
        $el.append($("<option></option>")
                .attr("value", value).text(key));
    });
}


        
// main method

function main() {

    console.log("host/port=>", host, port);

    initializeLocalMediaStream();
}


main();

 