# focus listener for textareas
# since swipe to refresh is quite sensitive, we will disable it
# when we detect a user typing
# note that this extends passed having a keyboard opened,
# as a user may still be reviewing his/her post
# swiping should automatically be reset on refresh

_flashFocus = (e) ->
    element = e.target or e.srcElement
    console.log "Flash focus", element.tagName
    if element.tagName == "TEXTAREA"
        Flash?.disableSwipeRefresh true
    return

_flashBlur = (e) ->
    element = e.target or e.srcElement
    console.log "Flash blur", element.tagName
    Flash?.disableSwipeRefresh false
    return

document.addEventListener "focus", _flashFocus, true
document.addEventListener "blur", _flashBlur, true
