// @ts-nocheck TypeScript 无法正确检查 iframe 注入的脚本字符串内容
import { ref, onUnmounted, type Ref } from 'vue'

export interface SelectedElement {
  tagName: string
  id?: string
  className?: string
  textContent?: string
  outerHTML: string
  xpath: string
}

export interface VisualEditorOptions {
  iframeRef: Ref<HTMLIFrameElement | null>
  onElementSelected?: (element: SelectedElement) => void
  onExitEditMode?: () => void
}

// iframe 通信消息类型
type ParentMessage =
  | { type: 'EDIT_MODE_ON' }
  | { type: 'EDIT_MODE_OFF' }
  | { type: 'CLEAR_SELECTION' }

type IframeMessage =
  | { type: 'ELEMENT_HOVER'; element: { tagName: string; id?: string; className?: string; xpath: string } }
  | { type: 'ELEMENT_CLICK'; element: { tagName: string; id?: string; className?: string; textContent?: string; outerHTML: string; xpath: string } }
  | { type: 'EDIT_MODE_READY' }

export function useVisualEditor(options: VisualEditorOptions) {
  const { iframeRef, onElementSelected, onExitEditMode } = options

  const isEditMode = ref(false)
  const selectedElements = ref<SelectedElement[]>([])
  const hoveredElement = ref<{ tagName: string; id?: string; className?: string; xpath: string } | null>(null)

  let messageHandler: ((event: MessageEvent) => void) | null = null
  let scriptInjected = false

  // 检查 iframe 是否可以访问
  const canAccessIframe = (): boolean => {
    const iframe = iframeRef.value
    if (!iframe) return false
    try {
      // 尝试访问 iframe 的 contentDocument 或 contentWindow
      const doc = iframe.contentDocument
      const win = iframe.contentWindow
      // 如果能访问但文档为 null，说明还没加载
      if (doc === null || win === null) return false
      return true
    } catch (e) {
      // 跨域访问会抛出异常
      console.warn('Cannot access iframe contentDocument:', e)
      return false
    }
  }

  // 注入编辑模式脚本到 iframe
  const injectEditModeScript = (): boolean => {
    const iframe = iframeRef.value
    if (!iframe) {
      console.error('injectEditModeScript: iframe is null')
      return false
    }

    try {
      const doc = iframe.contentDocument
      if (!doc) {
        console.error('injectEditModeScript: iframe contentDocument is null')
        return false
      }

      // 检查是否已注入
      if (doc.getElementById('visual-edit-script')) {
        console.log('injectEditModeScript: script already injected')
        return true
      }

      // 构建脚本内容
      const cssStyles = '.visual-edit-hover { outline: 2px dashed #1890ff !important; } ' +
        '.visual-edit-selected { outline: 3px solid #1890ff !important; pointer-events: none; }'

      const scriptContent = '(function() { ' +
        'var editMode = false; ' +
        'var selectedElements = []; ' +
        'var style = document.createElement(\"style\"); ' +
        'style.textContent = \"' + cssStyles.replace(/'/g, '\\\u0027') + '\"; ' +
        'document.head.appendChild(style); ' +
        'function getXPath(el) { ' +
        '  if (!el || el === document.body) return \"/body\"; ' +
        '  var path = \"\"; ' +
        '  while (el && el !== document.body) { ' +
        '    var sibling = el; ' +
        '    var index = 1; ' +
        '    while (sibling && sibling.previousElementSibling) { ' +
        '      sibling = sibling.previousElementSibling; index++; ' +
        '    } ' +
        '    var tagName = el.tagName.toLowerCase(); ' +
        '    path = \"/\" + tagName + \"[\" + index + \"]\" + path; ' +
        '    el = el.parentElement; ' +
        '  } ' +
        '  return \"/html\" + path; ' +
        '} ' +
        'function getElementInfo(el) { ' +
        '  return { ' +
        '    tagName: el.tagName, ' +
        '    id: el.id || undefined, ' +
        '    className: el.className || undefined, ' +
        '    textContent: el.textContent ? el.textContent.trim().substring(0, 200) : undefined, ' +
        '    outerHTML: el.outerHTML.substring(0, 500), ' +
        '    xpath: getXPath(el) ' +
        '  }; ' +
        '} ' +
        'function clearHighlights() { ' +
        '  var els = document.querySelectorAll(\".visual-edit-hover, .visual-edit-selected\"); ' +
        '  for (var i = 0; i < els.length; i++) { ' +
        '    els[i].classList.remove(\"visual-edit-hover\", \"visual-edit-selected\"); ' +
        '  } ' +
        '} ' +
        'document.addEventListener(\"mouseover\", function(e) { ' +
        '  if (!editMode) return; ' +
        '  var target = e.target; ' +
        '  if (target && target.tagName && target.tagName !== \"SCRIPT\" && target.tagName !== \"STYLE\") { ' +
        '    clearHighlights(); ' +
        '    target.classList.add(\"visual-edit-hover\"); ' +
        '    parent.postMessage({ type: \"ELEMENT_HOVER\", element: { tagName: target.tagName, id: target.id || undefined, className: target.className || undefined, xpath: getXPath(target) } }, \"*\"); ' +
        '  } ' +
        '}, true); ' +
        'document.addEventListener(\"mouseout\", function(e) { ' +
        '  if (!editMode) return; ' +
        '  var target = e.target; ' +
        '  if (target && target.classList && target.classList.contains(\"visual-edit-hover\")) { ' +
        '    target.classList.remove(\"visual-edit-hover\"); ' +
        '  } ' +
        '}, true); ' +
        'document.addEventListener(\"click\", function(e) { ' +
        '  if (!editMode) return; ' +
        '  e.preventDefault(); e.stopPropagation(); ' +
        '  var target = e.target; ' +
        '  if (target && target.tagName && target.tagName !== \"SCRIPT\" && target.tagName !== \"STYLE\") { ' +
        '    var prevSelected = document.querySelectorAll(\".visual-edit-selected\"); ' +
        '    for (var i = 0; i < prevSelected.length; i++) { prevSelected[i].classList.remove(\"visual-edit-selected\"); } ' +
        '    target.classList.add(\"visual-edit-selected\"); ' +
        '    parent.postMessage({ type: \"ELEMENT_CLICK\", element: getElementInfo(target) }, \"*\"); ' +
        '  } ' +
        '}, true); ' +
        'window.addEventListener(\"message\", function(e) { ' +
        '  var data = e.data; ' +
        '  if (!data || !data.type) return; ' +
        '  if (data.type === \"EDIT_MODE_ON\") { ' +
        '    editMode = true; ' +
        '    document.body.style.cursor = \"crosshair\"; ' +
        '    parent.postMessage({ type: \"EDIT_MODE_READY\" }, \"*\"); ' +
        '  } else if (data.type === \"EDIT_MODE_OFF\") { ' +
        '    editMode = false; ' +
        '    selectedElements = []; ' +
        '    clearHighlights(); ' +
        '    document.body.style.cursor = \"\"; ' +
        '  } else if (data.type === \"CLEAR_SELECTION\") { ' +
        '    clearHighlights(); ' +
        '  } ' +
        '}); ' +
        '})();'

      const scriptEl = doc.createElement('script')
      scriptEl.id = 'visual-edit-script'
      scriptEl.textContent = scriptContent
      doc.head.appendChild(scriptEl)

      console.log('injectEditModeScript: Script injected successfully')
      scriptInjected = true
      return true
    } catch (error) {
      console.error('injectEditModeScript: Failed to inject script:', error)
      return false
    }
  }

  // 等待 iframe 加载完成然后注入脚本
  const waitForIframeAndInject = () => {
    const iframe = iframeRef.value
    if (!iframe) {
      console.error('waitForIframeAndInject: iframe is null')
      return
    }

    // 如果 iframe 已经加载完成，直接注入
    if (iframe.contentDocument && iframe.contentDocument.readyState === 'complete') {
      console.log('waitForIframeAndInject: iframe already complete, injecting')
      injectEditModeScript()
      return
    }

    // 等待加载完成
    const onLoad = () => {
      console.log('waitForIframeAndInject: iframe loaded')
      injectEditModeScript()
    }

    iframe.addEventListener('load', onLoad, { once: true })

    // 如果 iframe 还没有 src，可能需要等待
    if (!iframe.getAttribute('src') && !iframe.src) {
      console.log('waitForIframeAndInject: iframe has no src yet')
    }
  }

  // 进入编辑模式
  const enterEditMode = () => {
    if (isEditMode.value) {
      console.log('enterEditMode: already in edit mode')
      return
    }

    console.log('enterEditMode: Entering edit mode')
    isEditMode.value = true
    scriptInjected = false

    // 尝试注入脚本
    if (canAccessIframe()) {
      console.log('enterEditMode: Can access iframe, trying to inject')
      const success = injectEditModeScript()
      if (success) {
        // 脚本注入成功，发送消息
        sendMessageToIframe({ type: 'EDIT_MODE_ON' })
      } else {
        // 注入失败，等待 iframe 加载后再试
        console.log('enterEditMode: Injection failed, waiting for iframe')
        waitForIframeAndInject()
        // 延迟发送消息，确保脚本已注入
        setTimeout(() => {
          sendMessageToIframe({ type: 'EDIT_MODE_ON' })
        }, 500)
      }
    } else {
      console.log('enterEditMode: Cannot access iframe, waiting')
      waitForIframeAndInject()
      // 延迟发送消息
      setTimeout(() => {
        sendMessageToIframe({ type: 'EDIT_MODE_ON' })
      }, 1000)
    }
  }

  // 发送消息给 iframe
  const sendMessageToIframe = (message: ParentMessage) => {
    const iframe = iframeRef.value
    if (!iframe || !iframe.contentWindow) {
      console.error('sendMessageToIframe: iframe or contentWindow is null')
      return
    }

    try {
      iframe.contentWindow.postMessage(message, '*')
      console.log('sendMessageToIframe: Sent message', message.type)
    } catch (error) {
      console.error('sendMessageToIframe: Failed to send message:', error)
    }
  }

  // 退出编辑模式
  const exitEditMode = () => {
    if (!isEditMode.value) return
    console.log('exitEditMode: Exiting edit mode')
    isEditMode.value = false
    hoveredElement.value = null
    sendMessageToIframe({ type: 'EDIT_MODE_OFF' })
    onExitEditMode?.()
  }

  // 清除选中元素
  const clearSelection = () => {
    selectedElements.value = []
    hoveredElement.value = null
    sendMessageToIframe({ type: 'CLEAR_SELECTION' })
  }

  // 移除单个选中元素
  const removeElement = (index: number) => {
    if (index >= 0 && index < selectedElements.value.length) {
      selectedElements.value.splice(index, 1)
    }
  }

  // 处理来自 iframe 的消息
  const handleIframeMessage = (event: MessageEvent) => {
    const iframe = iframeRef.value
    if (!iframe?.contentWindow) return

    // 确保消息来自预期的 iframe
    if (event.source !== iframe.contentWindow) return

    const data = event.data
    if (!data || !data.type) return

    console.log('handleIframeMessage: Received message', data.type)

    switch (data.type) {
      case 'ELEMENT_HOVER':
        if (isEditMode.value && data.element) {
          hoveredElement.value = data.element
        }
        break

      case 'ELEMENT_CLICK':
        if (isEditMode.value && data.element) {
          const selected: SelectedElement = {
            tagName: data.element.tagName,
            id: data.element.id,
            className: data.element.className,
            textContent: data.element.textContent,
            outerHTML: data.element.outerHTML,
            xpath: data.element.xpath
          }

          // 检查是否已选中该元素
          const exists = selectedElements.value.some(
            el => el.xpath === selected.xpath
          )
          if (!exists) {
            selectedElements.value.push(selected)
            console.log('handleIframeMessage: Element selected', selected.tagName)
            onElementSelected?.(selected)
          }
        }
        break

      case 'EDIT_MODE_READY':
        console.log('handleIframeMessage: Edit mode ready')
        break
    }
  }

  // 设置消息监听
  const setupMessageListener = () => {
    if (messageHandler) return
    messageHandler = handleIframeMessage
    window.addEventListener('message', messageHandler)
    console.log('setupMessageListener: Message listener set up')
  }

  // 移除消息监听
  const removeMessageListener = () => {
    if (messageHandler) {
      window.removeEventListener('message', messageHandler)
      messageHandler = null
    }
  }

  // 初始化
  setupMessageListener()

  // 清理
  onUnmounted(() => {
    removeMessageListener()
    if (isEditMode.value) {
      sendMessageToIframe({ type: 'EDIT_MODE_OFF' })
    }
  })

  return {
    isEditMode,
    selectedElements,
    hoveredElement,
    enterEditMode,
    exitEditMode,
    clearSelection,
    removeElement
  }
}