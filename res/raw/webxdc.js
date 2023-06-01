window.webxdc = (() => {
  let setUpdateListenerPromise = null
  var update_listener = () => {};
  var last_serial = 0;

  window.__webxdcUpdate = () => {
    var updates = JSON.parse(InternalJSApi.getStatusUpdates(last_serial));
    updates.forEach((update) => {
        update_listener(update);
        last_serial = update.serial;
    });
    if (setUpdateListenerPromise) {
      setUpdateListenerPromise()
      setUpdateListenerPromise = null
    }
  };

  return {
    selfAddr: InternalJSApi.selfAddr(),

    selfName: InternalJSApi.selfName(),

    setUpdateListener: (cb, serial) => {
        last_serial = typeof serial === "undefined" ? 0 : parseInt(serial);
        update_listener = cb
        var promise = new Promise((res, _rej) => {
          setUpdateListenerPromise = res
        })
        window.__webxdcUpdate();
        return promise
    },

    // deprecated 2022-02-20 all updates are returned through the callback set by setUpdateListener
    getAllUpdates: () => {
      console.error("deprecated 2022-02-20 all updates are returned through the callback set by setUpdateListener")
      return Promise.resolve([]);
    },

    sendUpdate: (payload, descr) => {
      InternalJSApi.sendStatusUpdate(JSON.stringify(payload), descr);
    },

    sendToChat: async (message) => {
        const data = {};
        if (!message.text && !message.file) {
            return Promise.reject("Invalid empty message, at least one of text or file should be provided");
        }
        if (message.text) {
            data.text = message.text;
        }
        if (message.file) {
            if (!message.file.name || typeof message.file.base64 !== 'string') {
                return Promise.reject("provided file is invalid, you need to set both name and base64 content");
            }
            data.base64 = message.file.base64;
            data.name = message.file.name;
        }


        const errorMsg = InternalJSApi.sendToChat(JSON.stringify(data));
        if (errorMsg) {
            return Promise.reject(errorMsg);
        }
    },
  };
})();
