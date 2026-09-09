import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js/lib/core'
import javascript from 'highlight.js/lib/languages/javascript'
import css from 'highlight.js/lib/languages/css'
import xml from 'highlight.js/lib/languages/xml'

hljs.registerLanguage('javascript', javascript)
hljs.registerLanguage('css', css)
hljs.registerLanguage('xml', xml)

const baseMd = new MarkdownIt()
const escapeHtml = baseMd.utils.escapeHtml

const markdown = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true,
  highlight(code: string, lang: string) {
    return highlightCode(code, lang)
  }
})

const normalizeLanguage = (lang?: string) => {
  if (!lang) return undefined
  const lower = lang.trim().toLowerCase()
  if (lower === 'html') return 'xml'
  if (lower === 'js') return 'javascript'
  if (lower === 'css') return 'css'
  return lower
}

const highlightCode = (code: string, lang?: string) => {
  const normalizedLanguage = normalizeLanguage(lang)

  if (normalizedLanguage && hljs.getLanguage(normalizedLanguage)) {
    try {
      const highlighted = hljs.highlight(code, {
        language: normalizedLanguage,
        ignoreIllegals: true
      }).value

      return `<pre class="md-code-block hljs"><code class="language-${normalizedLanguage}">${highlighted}</code></pre>`
    } catch (error) {
      console.warn('highlight.js 渲染失败:', error)
    }
  }

  return `<pre class="md-code-block"><code>${escapeHtml(code)}</code></pre>`
}

export const renderMarkdown = (content: string) => {
  return markdown.render(content)
}
