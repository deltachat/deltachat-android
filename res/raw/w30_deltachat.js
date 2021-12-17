window.deltachat = (() => {
  var update_listener = () => {};

  window.__w30update = (msgId) => {
    var update = W30.getStatusUpdate(msgId);
    if (update) {
      update_listener(JSON.parse(update));
    }
  };

  return {
    getChatName: () => W30.getChatName(),
    setStatusUpdateListener: (cb) => (update_listener = cb),
    getAllStatusUpdates: () => {
      return JSON.parse(W30.getAllStatusUpdates());
    },
    sendStatusUpdate: (description, payload) => {
      W30.sendStatusUpdate(description, payload);
    },
  };
})();
