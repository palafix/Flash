"use strict";

(function () {
  // we will handle media events
  var _flashMediaClick;

  _flashMediaClick = function _flashMediaClick(e) {
    var dataStore, element, i, ref, url;
    element = e.target || e.srcElement;
    if (!(element != null ? (ref = element.dataset.sigil) != null ? ref.toLowerCase().includes("inlinevideo") : void 0 : void 0)) {
      return;
    }
    i = 0;
    while (!element.hasAttribute("data-store")) {
      if (++i > 2) {
        return;
      }
      element = element.parentNode;
    }
    try {
      dataStore = JSON.parse(element.dataset.store);
    } catch (error) {
      e = error;
      return;
    }
    url = dataStore.src;
    if (!url || !url.startsWith("http")) {
      return;
    }
    console.log("Inline video " + url);
    if (typeof Flash !== "undefined" && Flash !== null ? Flash.loadVideo(url, dataStore.animatedGifVideo) : void 0) {
      e.stopPropagation();
      e.preventDefault();
    }
  };

  document.addEventListener("click", _flashMediaClick, true);
}).call(undefined);