window.deltachat = (() => {
  var update_listener = () => {};

  window.__w30update = (statusUpdateId) => {
    var update = W30.getStatusUpdate(statusUpdateId);
    if (update) {
      update_listener(JSON.parse(update));
    }
  };

  return {
    selfAddr: () => W30.selfAddr(),
    setStatusUpdateListener: (cb) => (update_listener = cb),
    getAllStatusUpdates: () => {
      return JSON.parse(W30.getAllStatusUpdates());
    },
    sendStatusUpdate: (description, payload) => {
      W30.sendStatusUpdate(description, payload);
    },
  };
})();
