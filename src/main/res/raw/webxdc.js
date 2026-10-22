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

  // File System Access API subset with per-app folder grants
  //
  // Important notes:
  // - `getFile({streaming: true})` is a webxdc extension returning a file-like object,
  //   which `URL.createObjectURL()` maps to a streamable URL.
  // - The lazy object is **not** a Blob subclass.
  // - Handles cannot be stored in IndexedDB. Use window.webxdc.getGrantedFolders() at
  //   start up to restore remembered folders grants without UI; showDirectoryPicker() always asks.
  // - Which app can read which folder is enforced from Java.
  let folderAccessResolver = null;
  window.__webxdcFolderResult = (index, name) => {
    if (folderAccessResolver) {
      folderAccessResolver(index < 0 ? null : { index: index, name: name });
      folderAccessResolver = null;
    }
  };

  const fileAccessDenied = (message) => new DOMException(message, "NotAllowedError");
  const fileNotFound = () => new DOMException("File not found", "NotFoundError");
  const readOnlyPermission = (desc) => (desc && desc.mode === "readwrite" ? "denied" : "granted");

  const sharedFileUrl = (index, path) =>
    "/__webxdc_shared__/" + index + "/" + path.split("/").map(encodeURIComponent).join("/");

  const lazyFileUrls = new WeakMap();
  const origCreateObjectURL = URL.createObjectURL.bind(URL);
  // Sliced streaming file doesn't work with URL.createObjectURL()
  URL.createObjectURL = (obj) => {
    const url = lazyFileUrls.get(obj);
    return url !== undefined ? url : origCreateObjectURL(obj);
  };

  const makeLazyFile = (index, path, name, size, type, lastModified, rangeStart, rangeEnd) => {
    const start = rangeStart === undefined ? 0 : rangeStart;
    const end = rangeEnd === undefined ? size : rangeEnd;
    const fetchFile = () => {
      if (start === 0 && end === size) {
        return fetch(sharedFileUrl(index, path));
      }
      return fetch(sharedFileUrl(index, path), {
        headers: { Range: "bytes=" + start + "-" + (end - 1) },
      });
    };
    const file = {
      name: name,
      type: type,
      lastModified: lastModified,
      size: Math.max(end - start, 0),
      slice: (sliceStart, sliceEnd, contentType) => {
        const len = file.size;
        const from =
          sliceStart === undefined ? 0 : sliceStart < 0 ? Math.max(len + sliceStart, 0) : Math.min(sliceStart, len);
        const to =
          sliceEnd === undefined ? len : sliceEnd < 0 ? Math.max(len + sliceEnd, 0) : Math.min(sliceEnd, len);
        return makeLazyFile(
          index, path, name, size, contentType === undefined ? type : contentType,
          lastModified, start + from, start + Math.max(from, to));
      },
      arrayBuffer: async () => {
        if (file.size === 0) {
          return new ArrayBuffer(0);
        }
        const response = await fetchFile();
        if (!response.ok) {
          throw fileNotFound();
        }
        return response.arrayBuffer();
      },
      text: async () => new TextDecoder().decode(await file.arrayBuffer()),
      stream: () => {
        let reader = null;
        return new ReadableStream({
          async pull(controller) {
            if (file.size === 0) {
              controller.close();
              return;
            }
            if (!reader) {
              const response = await fetchFile();
              if (!response.ok || !response.body) {
                controller.error(fileNotFound());
                return;
              }
              reader = response.body.getReader();
            }
            const { done, value } = await reader.read();
            if (done) {
              controller.close();
            } else {
              controller.enqueue(value);
            }
          },
          cancel: (reason) => (reader ? reader.cancel(reason) : undefined),
        });
      },
    };
    if (start === 0 && end === size) {
      lazyFileUrls.set(file, sharedFileUrl(index, path));
    }
    return file;
  };

  const makeFileHandle = (index, path, name, size, lastModified, type) => ({
    kind: "file",
    name: name,
    getFile: async (options = {}) => {
      if (options.streaming) {
        return makeLazyFile(index, path, name, size, type, lastModified);
      }
      const response = await fetch(sharedFileUrl(index, path));
      if (!response.ok) {
        throw fileNotFound();
      }
      return new File([await response.blob()], name, { type: type, lastModified: lastModified });
    },
    createWritable: async () => {
      throw fileAccessDenied("Read-only access");
    },
    queryPermission: readOnlyPermission,
    requestPermission: readOnlyPermission,
  });

  const makeDirectoryHandle = (index, path, name) => {
    const findChild = async (kind, childName, options = {}) => {
      if (options.create) {
        throw fileAccessDenied("Read-only access");
      }
      for await (const child of handle.values()) {
        if (child.kind === kind && child.name === childName) {
          return child;
        }
      }
      throw kind === "file" ? fileNotFound() : new DOMException("Directory not found", "NotFoundError");
    };
    const handle = {
      kind: "directory",
      name: name,
      values: async function* () {
        const entries = JSON.parse(InternalJSApi.listSharedDir(index, path));
        for (const entry of entries) {
          yield entry.isDirectory
            ? makeDirectoryHandle(index, entry.path, entry.name)
            : makeFileHandle(index, entry.path, entry.name, entry.size, entry.lastModified, entry.type);
        }
      },
      getDirectoryHandle: (dirName, options) => findChild("directory", dirName, options),
      getFileHandle: (fileName, options) => findChild("file", fileName, options),
      removeEntry: async () => {
        throw fileAccessDenied("Read-only access");
      },
      queryPermission: readOnlyPermission,
      requestPermission: readOnlyPermission,
    };
    return handle;
  };

  window.showDirectoryPicker = (options = {}) => {
    if (options.mode === "readwrite") {
      return Promise.reject(fileAccessDenied("Read-only access"));
    }
    if (folderAccessResolver) {
      return Promise.reject(new DOMException("Picker is already open", "AbortError"));
    }
    return new Promise((resolve, reject) => {
      folderAccessResolver = (folder) => {
        if (folder) {
          resolve(makeDirectoryHandle(folder.index, "", folder.name));
        } else {
          reject(new DOMException("User aborted", "AbortError"));
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

    getGrantedFolders: async () =>
          JSON.parse(InternalJSApi.listGrantedFolders()).map((folder) =>
            makeDirectoryHandle(folder.index, "", folder.name)),
  };
})();
