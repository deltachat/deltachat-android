window.deltachat = (() => {
  var update_listener = () => {};

  window.__w30update = (statusUpdateId) => {
    var updates = JSON.parse(InternalJSApi.getStatusUpdates(statusUpdateId));
    if (updates.lenght == 1) {
      update_listener(updates[0]);
    }
  };

  return {
    selfAddr: () => InternalJSApi.selfAddr(),
    setStatusUpdateListener: (cb) => (update_listener = cb),
    getAllStatusUpdates: () => {
      return JSON.parse(InternalJSApi.getStatusUpdates(0));
    },
    sendStatusUpdate: (description, payload) => {
      InternalJSApi.sendStatusUpdate(description, payload);
    },
  };
})();
