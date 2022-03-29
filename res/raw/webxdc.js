window.webxdc = (() => {
  var update_listener = () => {};
  var last_serial = 0;

  window.__webxdcUpdate = () => {
    var updates = JSON.parse(InternalJSApi.getStatusUpdates(last_serial));
    updates.forEach((update) => {
        update_listener(update);
        last_serial = update.serial;
    });
  };

  return {
    selfAddr: InternalJSApi.selfAddr(),

    selfName: InternalJSApi.selfName(),

    setUpdateListener: (cb, serial) => {
        last_serial = typeof serial === "undefined" ? 0 : parseInt(serial);
        update_listener = cb
        window.__webxdcUpdate();
    },

    // deprecated 2022-02-20 all updates are returned through the callback set by setUpdateListener
    getAllUpdates: () => {
      console.error("deprecated 2022-02-20 all updates are returned through the callback set by setUpdateListener")
      return Promise.resolve([]);
    },

    sendUpdate: (payload, descr) => {
      InternalJSApi.sendStatusUpdate(JSON.stringify(payload), descr);
    },
  };
})();
