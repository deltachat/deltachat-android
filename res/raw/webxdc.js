window.webxdc = (() => {
  var update_listener = () => {};

  window.__webxdcUpdate = (updateId) => {
    var updates = JSON.parse(InternalJSApi.getStatusUpdates(updateId));
    if (updates.length === 1) {
      update_listener(updates[0]);
    }
  };

  return {
    selfAddr: () => InternalJSApi.selfAddr(),

    setUpdateListener: (cb) => (update_listener = cb),

    getAllUpdates: () => {
      return JSON.parse(InternalJSApi.getStatusUpdates(0));
    },

    sendUpdate: (description, payload) => {
      InternalJSApi.sendStatusUpdate(description, JSON.stringify(payload));
    },
  };
})();
