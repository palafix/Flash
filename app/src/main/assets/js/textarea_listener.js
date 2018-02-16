"use strict";

(function () {
  // focus listener for textareas
  // since swipe to refresh is quite sensitive, we will disable it
  // when we detect a user typing
  // note that this extends passed having a keyboard opened,
  // as a user may still be reviewing his/her post
  // swiping should automatically be reset on refresh
  var _flashBlur, _flashFocus;

  _flashFocus = function _flashFocus(e) {
    var element;
    element = e.target || e.srcElement;
    console.log("Flash focus", element.tagName);
    if (element.tagName === "TEXTAREA") {
      if (typeof Flash !== "undefined" && Flash !== null) {
        Flash.disableSwipeRefresh(true);
      }
    }
  };

  _flashBlur = function _flashBlur(e) {
    var element;
    element = e.target || e.srcElement;
    console.log("Flash blur", element.tagName);
    if (typeof Flash !== "undefined" && Flash !== null) {
      Flash.disableSwipeRefresh(false);
    }
  };

  document.addEventListener("focus", _flashFocus, true);

  document.addEventListener("blur", _flashBlur, true);
}).call(undefined);