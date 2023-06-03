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
      if (!message.file && !message.text) {
        return Promise.reject("sendToChat() error: file or text missing");
      }
      /** @type {(file: Blob) => Promise<string>} */
      const blobToBase64 = (file) => {
        const dataStart = ";base64,";
        return new Promise((resolve, reject) => {
          const reader = new FileReader();
          reader.readAsDataURL(file);
          reader.onload = () => {
            /** @type {string} */
            //@ts-ignore
            let data = reader.result;
            resolve(data.slice(data.indexOf(dataStart) + dataStart.length));
          };
          reader.onerror = () => reject(reader.error);
        });
      };
      if (message.text) {
        data.text = message.text;
      }

      /** @type {{file_name: string, file_message: string} | null} */
      if (message.file) {
        let base64content;
        if (!message.file.name) {
          return Promise.reject("sendToChat() error: file name missing");
        }
        if (
          Object.keys(message.file).filter((key) =>
            ["blob", "base64", "plainText"].includes(key)
          ).length > 1
        ) {
          return Promise.reject("sendToChat() error: only one of blob, base64 or plainText allowed");
        }

        // @ts-ignore - needed because typescript imagines that blob would not exist
        if (message.file.blob instanceof Blob) {
          // @ts-ignore - needed because typescript imagines that blob would not exist
          base64content = await blobToBase64(message.file.blob);
          // @ts-ignore - needed because typescript imagines that base64 would not exist
        } else if (typeof message.file.base64 === "string") {
          // @ts-ignore - needed because typescript imagines that base64 would not exist
          base64content = message.file.base64;
          // @ts-ignore - needed because typescript imagines that plainText would not exist
        } else if (typeof message.file.plainText === "string") {
          base64content = await blobToBase64(
            // @ts-ignore - needed because typescript imagines that plainText would not exist
            new Blob([message.file.plainText])
          );
        } else {
          return Promise.reject("sendToChat() error: none of blob, base64 or plainText set correctly");
        }
        data.base64 = base64content;
        data.name = message.file.name;
      }

      const errorMsg = InternalJSApi.sendToChat(JSON.stringify(data));
      if (errorMsg) {
        return Promise.reject(errorMsg);
      }
    },
  };
})();
