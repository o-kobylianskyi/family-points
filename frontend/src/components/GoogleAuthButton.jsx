import { useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'

// Google owns the iframe. Set both its script language and renderButton locale;
// the text must follow FamilyPoints' language, not the Google account language.
export default function GoogleAuthButton({ onCredential, disabled = false }) {
  const { t, i18n } = useTranslation()
  const clientId = import.meta.env.VITE_GOOGLE_CLIENT_ID?.trim()
  const location = window.location
  const isSecureOrigin = location.protocol === 'https:' ||
    (location.protocol === 'http:' && ['localhost', '127.0.0.1'].includes(location.hostname))
  const language = (i18n.resolvedLanguage || i18n.language || 'uk').split('-')[0]
  const locale = ['uk', 'de', 'en', 'ru'].includes(language) ? language : 'en'
  const container = useRef(null)
  const callback = useRef(onCredential)
  const [error, setError] = useState('')
  callback.current = onCredential

  useEffect(() => {
    if (!clientId || !isSecureOrigin || !container.current) return
    let cancelled = false
    let observer
    let themeObserver
    let lastWidth = 0
    let lastTheme = ''
    const host = container.current

    const render = () => {
      if (cancelled || !window.google?.accounts?.id) return
      window.google.accounts.id.initialize({
        client_id: clientId,
        ux_mode: 'popup',
        auto_select: false,
        callback: result => { if (!cancelled && result.credential) callback.current(result.credential) },
      })

      const updateWidth = () => {
        if (cancelled || !host.isConnected) return
        const width = Math.min(400, Math.floor(host.getBoundingClientRect().width))
        const buttonTheme = document.documentElement.dataset.theme === 'dark' ? 'filled_black' : 'outline'
        if (width < 120 || (width === lastWidth && buttonTheme === lastTheme)) return
        lastWidth = width
        lastTheme = buttonTheme
        host.replaceChildren()
        window.google.accounts.id.renderButton(host, {
          type: 'standard', theme: buttonTheme, size: 'large',
          text: 'continue_with', width, locale,
        })
      }
      updateWidth()
      observer = new ResizeObserver(updateWidth)
      observer.observe(host)
      themeObserver = new MutationObserver(updateWidth)
      themeObserver.observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme'] })
    }

    // The SDK is cached globally by Google. Recreate the script with ?hl when
    // FamilyPoints language changes, so a previous Russian/English GIS session
    // cannot silently override the selected language.
    document.querySelectorAll('script[data-family-points-google-gis]').forEach(script => script.remove())
    const script = document.createElement('script')
    script.dataset.familyPointsGoogleGis = locale
    script.src = 'https://accounts.google.com/gsi/client?hl=' + encodeURIComponent(locale)
    script.async = true
    script.defer = true
    script.onload = render
    script.onerror = () => { if (!cancelled) setError(t('authUi.googleUnavailable')) }
    document.head.appendChild(script)

    return () => {
      cancelled = true
      observer?.disconnect()
      themeObserver?.disconnect()
      script.remove()
      host.replaceChildren()
    }
  }, [clientId, isSecureOrigin, locale, t])

  if (!clientId) return <p className="auth-provider-notice" role="status">{t('authUi.googleNotConfigured')}</p>
  if (!isSecureOrigin) return <p className="auth-provider-notice" role="status">{t('authUi.googleSecureOrigin')}</p>
  return (
    <div className="google-auth-button" aria-busy={disabled}>
      <div ref={container} className="google-auth-button-frame"
        style={{ opacity: disabled ? 0.5 : 1, pointerEvents: disabled ? 'none' : 'auto' }} />
      {error && <small role="alert">{error}</small>}
    </div>
  )
}
