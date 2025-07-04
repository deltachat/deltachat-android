// keep the "hello from INIT_SCRIPT" log line as long as we are testing
// to easily see that it is called from every frame in the chrome dev console
console.log("hello from INIT_SCRIPT");

// we remove peer connection by overwriting api
try {
    window.RTCPeerConnection = () => {};
    RTCPeerConnection = () => {};
} catch (e) {
    console.error("failed to overwrite RTCPeerConnection apis",e);
};

try {
    // webkitRTCPeerConnection was the previous name, also in chrome, not only in webkit
    window.webkitRTCPeerConnection = () => {};
    webkitRTCPeerConnection = () => {};
} catch (e) {};

console.log("bye from INIT_SCRIPT");
