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

  // Motion sensor permission prompt.
  if (InternalJSApi.getSensorPermissionState() === "ask") {
    let sensorPermissionResolver = null;
    let sensorAskPending = false;
    const askSensorPermission = () => {
      if (!sensorAskPending) {
        sensorAskPending = true;
        InternalJSApi.requestSensorPermission();
      }
    };
    const SENSOR_EVENTS = ["deviceorientation", "devicemotion", "deviceorientationabsolute"];
    const origAddEventListener = EventTarget.prototype.addEventListener;
    EventTarget.prototype.addEventListener = function (type, ...args) {
      if (SENSOR_EVENTS.includes(type)) {
        askSensorPermission();
      }
      return origAddEventListener.call(this, type, ...args);
    };
    for (const property of ["ondeviceorientation", "ondevicemotion", "ondeviceorientationabsolute"]) {
      let handler = null;
      Object.defineProperty(window, property, {
        get: () => handler,
        set: (h) => {
          askSensorPermission();
          handler = h;
        },
        configurable: true,
      });
    }
    window.__webxdcSensorPermission = (granted) => {
      sensorAskPending = false;
      if (sensorPermissionResolver) {
        sensorPermissionResolver(granted ? "granted" : "denied");
        sensorPermissionResolver = null;
      }
      if (granted) {
        location.reload();
      }
    };
    const requestSensorPermission = () =>
      new Promise((resolve) => {
        sensorPermissionResolver = resolve;
        askSensorPermission();
      });
    if (window.DeviceOrientationEvent && !DeviceOrientationEvent.requestPermission) {
      DeviceOrientationEvent.requestPermission = requestSensorPermission;
    }
    if (window.DeviceMotionEvent && !DeviceMotionEvent.requestPermission) {
      DeviceMotionEvent.requestPermission = requestSensorPermission;
    }
  }

  // File System Access API subset with permission grants. Polyfilled. Read-only.
  // File handles additionally carry a non-standard `url` for streaming:
  // getFile() loads the whole file into memory, audio or images shall use `url`.
  let folderAccessResolver = null;
  window.__webxdcFolderResult = (index, name) => {
    if (folderAccessResolver) {
      folderAccessResolver(index < 0 ? null : { index: index, name: name });
      folderAccessResolver = null;
    }
  };

  const fileAccessDenied = (message) => new DOMException(message, "NotAllowedError");

  const sharedFileUrl = (index, path) =>
    "/__webxdc_shared__/" + index + "/" + path.split("/").map(encodeURIComponent).join("/");

  const makeFileHandle = (index, path, name, lastModified) => {
    const url = sharedFileUrl(index, path);
    return {
      kind: "file",
      name: name,
      url: url,
      getFile: async () => {
        const response = await fetch(url);
        if (!response.ok) {
          throw new DOMException("File not found", "NotFoundError");
        }
        return new File([await response.blob()], name, { lastModified: lastModified });
      },
      createWritable: async () => {
        throw fileAccessDenied("Read-only");
      },
      queryPermission: () => "granted",
      requestPermission: () => "granted",
    };
  };

  const makeDirectoryHandle = (index, path, name) => {
    const handle = {
      kind: "directory",
      name: name,
      values: async function* () {
        const entries = JSON.parse(InternalJSApi.listSharedDir(index, path));
        for (const entry of entries) {
          yield entry.isDirectory
            ? makeDirectoryHandle(index, entry.path, entry.name)
            : makeFileHandle(index, entry.path, entry.name, entry.lastModified);
        }
      },
      getDirectoryHandle: async (dirName, options = {}) => {
        if (options.create) {
          throw fileAccessDenied("Read-only");
        }
        for await (const child of handle.values()) {
          if (child.kind === "directory" && child.name === dirName) {
            return child;
          }
        }
        throw new DOMException("Directory not found", "NotFoundError");
      },
      getFileHandle: async (fileName, options = {}) => {
        if (options.create) {
          throw fileAccessDenied("Read-only");
        }
        for await (const child of handle.values()) {
          if (child.kind === "file" && child.name === fileName) {
            return child;
          }
        }
        throw new DOMException("file not found", "NotFoundError");
      },
      removeEntry: async () => {
        throw fileAccessDenied("Read-only");
      },
      queryPermission: () => "granted",
      requestPermission: () => "granted",
    };
    return handle;
  };

  window.showDirectoryPicker = (options = {}) => {
    if (options.mode === "readwrite") {
      return Promise.reject(fileAccessDenied("Read-only"));
    }
    if (folderAccessResolver) {
      return Promise.reject(new DOMException("Picker is already open", "AbortError"));
    }
    return new Promise((resolve, reject) => {
      folderAccessResolver = (folder) => {
        if (folder) {
          resolve(makeDirectoryHandle(folder.index, "", folder.name));
        } else {
          reject(new DOMException("the user aborted a request", "AbortError"));
        }
      };
      InternalJSApi.requestFolderAccess();
    });
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
        if (!(data instanceof Uint8Array)) {
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

    isAppSender: InternalJSApi.isAppSender(),

    isBroadcast: InternalJSApi.isBroadcast(),

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
