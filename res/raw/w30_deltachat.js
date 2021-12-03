window.deltachat = (() => {
  var update_listener = () => {};

  window.__w30update = (msgId) => {
    var update = W30.getStateUpdate(msgId);
    if (update) {
      update_listener(JSON.parse(update));
    }
  };

  return {
    getChatName: () => Promise.resolve(W30.getChatName()),
    setStateUpdateListener: (cb) => (update_listener = cb),
    getAllStateUpdates: () => {
      return Promise.resolve(JSON.parse(W30.getAllStateUpdates()));
    },
    sendStateUpdate: (description, payload) => {
      window.__w30update(W30.sendStateUpdate(description, payload));
      return Promise.resolve()
    },
  };
})();
