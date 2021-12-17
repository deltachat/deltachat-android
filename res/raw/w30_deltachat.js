window.deltachat = (() => {
  var update_listener = () => {};

  window.__w30update = (statusUpdateId) => {
    var update = InternalJSApi.getStatusUpdate(statusUpdateId);
    if (update) {
      update_listener(JSON.parse(update));
    }
  };

  return {
    selfAddr: () => InternalJSApi.selfAddr(),
    setStatusUpdateListener: (cb) => (update_listener = cb),
    getAllStatusUpdates: () => {
      return JSON.parse(InternalJSApi.getAllStatusUpdates());
    },
    sendStatusUpdate: (description, payload) => {
      InternalJSApi.sendStatusUpdate(description, payload);
    },
  };
})();
