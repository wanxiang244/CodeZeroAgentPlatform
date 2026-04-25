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

  // 发送消息给 iframe
  const sendMessageToIframe = (message: ParentMessage) => {
    const iframe = iframeRef.value
    if (!iframe || !iframe.contentWindow) return

    // 等待 iframe 加载完成后再发送消息
    const sendWhenReady = () => {
      try {
        iframe.contentWindow?.postMessage(message, '*')
      } catch (error) {
        console.error('Failed to send message to iframe:', error)
      }
    }

    if (iframe.contentDocument?.readyState === 'complete') {
      sendWhenReady()
    } else {
      iframe.addEventListener('load', sendWhenReady, { once: true })
    }
  }

  // 注入编辑模式脚本到 iframe
  const injectEditModeScript = () => {
    const iframe = iframeRef.value
    if (!iframe || !iframe.contentDocument) return

    const doc = iframe.contentDocument
    // 检查是否已注入
    if (doc.getElementById('visual-edit-script')) return

    const script = doc.createElement('script')
    script.id = 'visual-edit-script'
    // 使用普通字符串避免 esbuild 解析模板字符串时的 CSS 括号问题
    const cssStyles = '.visual-edit-hover { outline: 2px dashed #1890ff !important; } ' +
      '.visual-edit-selected { outline: 3px solid #1890ff !important; pointer-events: none; }'
    const scriptContent: any = '(function() { ' +
      'let editMode = false; ' +
      'let selectedElements = []; ' +
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
      '  if (target && target.classList.contains(\"visual-edit-hover\")) { ' +
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
      '  if (data.type === \"EDIT_MODE_ON\") { editMode = true; document.body.style.cursor = \"crosshair\"; parent.postMessage({ type: \"EDIT_MODE_READY\" }, \"*\"); } ' +
      '  else if (data.type === \"EDIT_MODE_OFF\") { editMode = false; selectedElements = []; clearHighlights(); document.body.style.cursor = \"\"; } ' +
      '  else if (data.type === \"CLEAR_SELECTION\") { clearHighlights(); } ' +
      '}); ' +
      '})();'
    script.textContent = scriptContent
    doc.head.appendChild(script)

        // 生成元素 xpath
        function getXPath(el) {
          if (!el || el === doc.body) return '/body'
          let path = ''
          while (el && el !== doc.body) {
            let sibling = el
            let index = 1
            while (sibling && sibling.previousElementSibling) {
              sibling = sibling.previousElementSibling
              index++
            }
            const tagName = el.tagName.toLowerCase()
            path = '/' + tagName + '[' + index + ']' + path
            el = el.parentElement
          }
          return '/html' + path
        }

        // 获取元素信息
        function getElementInfo(el) {
          return {
            tagName: el.tagName,
            id: el.id || undefined,
            className: el.className || undefined,
            textContent: el.textContent?.trim().substring(0, 200),
            outerHTML: el.outerHTML.substring(0, 500),
            xpath: getXPath(el)
          }
        }

        // 清除所有高亮
        function clearHighlights() {
          doc.querySelectorAll('.visual-edit-hover, .visual-edit-selected').forEach(el => {
            el.classList.remove('visual-edit-hover', 'visual-edit-selected')
          })
        }

        // 处理鼠标移入
        doc.addEventListener('mouseover', function(e) {
          if (!editMode) return
          const target = e.target
          if (target && target.tagName && target.tagName !== 'SCRIPT' && target.tagName !== 'STYLE') {
            clearHighlights()
            target.classList.add('visual-edit-hover')
            parent.postMessage({
              type: 'ELEMENT_HOVER',
              element: { tagName: target.tagName, id: target.id || undefined, className: target.className || undefined, xpath: getXPath(target) }
            }, '*')
          }
        }, true)

        // 处理鼠标移出
        doc.addEventListener('mouseout', function(e) {
          if (!editMode) return
          const target = e.target
          if (target && target.classList.contains('visual-edit-hover')) {
            target.classList.remove('visual-edit-hover')
          }
        }, true)

        // 处理点击
        doc.addEventListener('click', function(e) {
          if (!editMode) return
          e.preventDefault()
          e.stopPropagation()

          const target = e.target
          if (target && target.tagName && target.tagName !== 'SCRIPT' && target.tagName !== 'STYLE') {
            // 移除之前的选择高亮
            doc.querySelectorAll('.visual-edit-selected').forEach(el => {
              el.classList.remove('visual-edit-selected')
            })
            target.classList.add('visual-edit-selected')
            parent.postMessage({ type: 'ELEMENT_CLICK', element: getElementInfo(target) }, '*')
          }
        }, true)

        // 监听来自父窗口的消息
        window.addEventListener('message', function(e) {
          const data = e.data
          if (!data || !data.type) return

          switch (data.type) {
            case 'EDIT_MODE_ON':
              editMode = true
              doc.body.style.cursor = 'crosshair'
              parent.postMessage({ type: 'EDIT_MODE_READY' }, '*')
              break
            case 'EDIT_MODE_OFF':
              editMode = false
              selectedElements = []
              clearHighlights()
              doc.body.style.cursor = ''
              break
            case 'CLEAR_SELECTION':
              clearHighlights()
              break
          }
        })
      }

  // 进入编辑模式
  const enterEditMode = () => {
    if (isEditMode.value) return
    isEditMode.value = true

    // 注入脚本到 iframe
    injectEditModeScript()

    // 发送编辑模式开启消息
    sendMessageToIframe({ type: 'EDIT_MODE_ON' })
  }

  // 退出编辑模式
  const exitEditMode = () => {
    if (!isEditMode.value) return
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

    const data = event.data as IframeMessage

    switch (data.type) {
      case 'ELEMENT_HOVER':
        if (isEditMode.value) {
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
            onElementSelected?.(selected)
          }
        }
        break

      case 'EDIT_MODE_READY':
        console.log('Iframe edit mode ready')
        break
    }
  }

  // 设置消息监听
  const setupMessageListener = () => {
    if (messageHandler) return
    messageHandler = handleIframeMessage
    window.addEventListener('message', messageHandler)
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