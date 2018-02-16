"use strict";

(function () {
  // bases the header contents if it exists
  var header;

  header = document.getElementById("mJewelNav");

  if (header !== null) {
    if (typeof Flash !== "undefined" && Flash !== null) {
      Flash.handleHeader(header.outerHTML);
    }
  }
}).call(undefined);