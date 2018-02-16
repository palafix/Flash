# we will handle media events
_flashMediaClick = (e) ->

  element = e.target or e.srcElement
  if !element?.dataset.sigil?.toLowerCase().includes("inlinevideo")
    return

  i = 0
  while !element.hasAttribute("data-store")
    if ++i > 2
      return
    element = element.parentNode

  try
    dataStore = JSON.parse(element.dataset.store)
  catch e
    return

  url = dataStore.src

  if !url || !url.startsWith("http")
    return

  console.log "Inline video #{url}"
  if Flash?.loadVideo url, dataStore.animatedGifVideo
    e.stopPropagation()
    e.preventDefault()
  return

document.addEventListener "click", _flashMediaClick, true