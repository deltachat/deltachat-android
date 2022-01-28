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

    selfName: () => InternalJSApi.selfName(),

    setUpdateListener: (cb) => (update_listener = cb),

    allUpdates: () => {
      return Promise.resolve(JSON.parse(InternalJSApi.getStatusUpdates(0)));
    },

    // deprecated, use `await updates = getUpdates()` instead
    getAllUpdates: () => {
      return JSON.parse(InternalJSApi.getStatusUpdates(0));
    },

    sendUpdate: (payload, descr) => {
      InternalJSApi.sendStatusUpdate(JSON.stringify(payload), descr);
    },
  };
})();
