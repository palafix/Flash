"use strict";

(function () {

  /*
   * On top of the click event, we must stop it for long presses
   * Since that will conflict with the context menu
   * Note that we only override it on conditions where the context menu
   * Will occur
   */
  var _flashAClick, _flashPreventClick, clickTimeout, prevented;

  prevented = false;

  _flashAClick = function _flashAClick(e) {
    /*
     * Commonality; check for valid target
     */
    var element, url;
    element = e.target || e.srcElement;
    if (element.tagName !== "A") {
      element = element.parentNode;
    }
    // Notifications is two layers under
    if (element.tagName !== "A") {
      element = element.parentNode;
    }
    if (element.tagName === "A") {
      if (!prevented) {
        url = element.getAttribute("href");
        console.log("Click Intercept " + url);
        // if Flash is injected, check if loading the url through an overlay works
        if ((typeof Flash !== "undefined" && Flash !== null ? Flash.loadUrl(url) : void 0) === true) {
          e.stopPropagation();
          e.preventDefault();
        }
      } else {
        console.log("Click Intercept Prevented");
      }
    }
  };

  _flashPreventClick = function _flashPreventClick() {
    console.log("Click prevented");
    prevented = true;
  };

  document.addEventListener("click", _flashAClick, true);

  clickTimeout = void 0;

  document.addEventListener("touchstart", function (e) {
    clickTimeout = setTimeout(_flashPreventClick, 400);
  }, true);

  document.addEventListener("touchend", function (e) {
    prevented = false;
    clearTimeout(clickTimeout);
  }, true);
}).call(undefined);