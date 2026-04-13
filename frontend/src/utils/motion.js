const REVEAL_SELECTOR = [
  '[data-reveal]',
  '.surface-card',
  '.dark-section',
  '.decor-divider'
].join(',')

const REVEAL_READY_ATTR = 'data-reveal-ready'

function prefersReducedMotion() {
  return typeof window !== 'undefined'
    && typeof window.matchMedia === 'function'
    && window.matchMedia('(prefers-reduced-motion: reduce)').matches
}

export function initGlobalMotion(router) {
  if (typeof window === 'undefined' || typeof document === 'undefined') {
    return () => {}
  }

  const reducedMotion = prefersReducedMotion()
  let revealObserver = null

  if (!reducedMotion && 'IntersectionObserver' in window) {
    revealObserver = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (!entry.isIntersecting) return
          entry.target.classList.add('is-revealed')
          revealObserver?.unobserve(entry.target)
        })
      },
      {
        threshold: 0.16,
        rootMargin: '0px 0px -10% 0px'
      }
    )
  }

  const markElement = (element) => {
    if (!(element instanceof HTMLElement)) return
    if (element.getAttribute(REVEAL_READY_ATTR) === '1') return
    element.setAttribute(REVEAL_READY_ATTR, '1')
    element.classList.add('reveal-on-scroll')
    if (reducedMotion || !revealObserver) {
      element.classList.add('is-revealed')
      return
    }
    revealObserver.observe(element)
  }

  const registerRevealTargets = (root = document) => {
    const targetRoot = root instanceof HTMLElement || root instanceof Document ? root : document
    targetRoot.querySelectorAll(REVEAL_SELECTOR).forEach(markElement)
  }

  registerRevealTargets(document)

  const mutationObserver = new MutationObserver((records) => {
    records.forEach((record) => {
      record.addedNodes.forEach((node) => {
        if (!(node instanceof HTMLElement)) return
        if (node.matches(REVEAL_SELECTOR)) {
          markElement(node)
        }
        registerRevealTargets(node)
      })
    })
  })

  mutationObserver.observe(document.body, { childList: true, subtree: true })

  const onRouteReady = () => {
    window.requestAnimationFrame(() => {
      window.requestAnimationFrame(() => {
        registerRevealTargets(document)
      })
    })
  }

  const removeAfterEach = typeof router?.afterEach === 'function'
    ? router.afterEach(() => {
      onRouteReady()
    })
    : null

  window.addEventListener('load', onRouteReady, { once: true })

  return () => {
    mutationObserver.disconnect()
    revealObserver?.disconnect()
    if (typeof removeAfterEach === 'function') {
      removeAfterEach()
    }
  }
}
