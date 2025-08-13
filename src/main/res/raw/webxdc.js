window.webxdc = (() => {
  // TTS
  window.speechSynthesis = {
    speak: (utterance) => {
      InternalJSApi.ttsSpeak(utterance.text, utterance.lang);
    },
    getVoices: () => {
      return [];
    },
  };
  window.SpeechSynthesisUtterance = function (text) {
    return {
      text: text,
      lang: "",
    };
  };

  let setUpdateListenerPromise = null;
  let update_listener = () => {};
  let last_serial = 0;
  let realtimeChannel = null;

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

  window.__webxdcRealtimeData = (intArray) => {
    if (realtimeChannel) {
      realtimeChannel.__receive(Uint8Array.from(intArray));
    }
  };

  const createRealtimeChannel = () => {
    let listener = null;
    return {
      setListener: (li) => listener = li,
      leave: () => InternalJSApi.leaveRealtimeChannel(),
      send: (data) => {
        if ((!data) instanceof Uint8Array) {
          throw new Error('realtime listener data must be a Uint8Array')
        }
        InternalJSApi.sendRealtimeData(JSON.stringify(Array.from(data)));
      },
      __receive: (data) => {
        if (listener) {
          listener(data);
        }
      },
    };
  }

  return {
    sendUpdateMaxSize: InternalJSApi.sendUpdateMaxSize(),

    sendUpdateInterval: InternalJSApi.sendUpdateInterval(),

    selfAddr: InternalJSApi.selfAddr(),

    selfName: InternalJSApi.selfName(),

    joinRealtimeChannel: () => {
      realtimeChannel = createRealtimeChannel();
      InternalJSApi.sendRealtimeAdvertisement();
      return realtimeChannel;
    },

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

    sendUpdate: (payload) => {
      InternalJSApi.sendStatusUpdate(JSON.stringify(payload));
    },

    sendToChat: async (message) => {
      const data = {};
      if (!message.file && !message.text) {
        return Promise.reject("sendToChat() error: file or text missing");
      }

      const blobToBase64 = (file) => {
        const dataStart = ";base64,";
        return new Promise((resolve, reject) => {
          const reader = new FileReader();
          reader.readAsDataURL(file);
          reader.onload = () => {
            let data = reader.result;
            resolve(data.slice(data.indexOf(dataStart) + dataStart.length));
          };
          reader.onerror = () => reject(reader.error);
        });
      };
      if (message.text) {
        data.text = message.text;
      }

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

        if (message.file.blob instanceof Blob) {
          base64content = await blobToBase64(message.file.blob);
        } else if (typeof message.file.base64 === "string") {
          base64content = message.file.base64;
        } else if (typeof message.file.plainText === "string") {
          base64content = await blobToBase64(new Blob([message.file.plainText]));
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

    importFiles: (filters) => {
        var element = document.createElement("input");
        element.type = "file";
        element.accept = [
            ...(filters.extensions || []),
            ...(filters.mimeTypes || []),
        ].join(",");
        element.multiple = filters.multiple || false;
        const promise = new Promise((resolve, _reject) => {
            element.onchange = (_ev) => {
                console.log("element.files", element.files);
                const files = Array.from(element.files || []);
                document.body.removeChild(element);
                resolve(files);
            };
        });
        element.style.display = "none";
        document.body.appendChild(element);
        element.click();
        console.log(element);
        return promise;
    },
  };
})();
