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

    sendToChat: (content) => {
        var data = {};

        if (content.text) {
            data.text = content.text;
        }

        if (content.file) {
            var file = content.file;
            var reader = new FileReader();
            reader.onload = function(readerEvt) {
                var binaryString = readerEvt.target.result;
                data.blob = btoa(binaryString);
                data.name = file.name;
                data.type = file.type;
                InternalJSApi.sendToChat(JSON.stringify(data));
            };
            reader.readAsBinaryString(file);
        } else {
            InternalJSApi.sendToChat(JSON.stringify(data));
        }
    },
  };
})();
