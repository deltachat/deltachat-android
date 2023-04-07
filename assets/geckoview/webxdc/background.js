"use strict";

function setupRedirect(fromUrl, redirectUrl) {
  browser.webRequest.onBeforeRequest.addListener(
    details => {
      console.log(`Extension redirects from ${fromUrl} to ${redirectUrl}`);
      return { redirectUrl };
    },
    { urls: [fromUrl] },
    ["blocking"]
  );
}

setupRedirect(
  "*://*/webxdc.js",
  browser.runtime.getURL("webxdc.js")
);
setupRedirect(
  "<all_urls>",
  browser.runtime.getURL("webxdc.html")
);
