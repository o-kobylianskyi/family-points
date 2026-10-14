import { useEffect, useRef, useState } from 'react'

// Google Identity Services; browser receives credential and server validates it.
export default function GoogleAuthButton({ onCredential, disabled = false }) {
  const clientId = import.meta.env.VITE_GOOGLE_CLIENT_ID
  const container = useRef(null)
  const callback = useRef(onCredential)
  const [error, setError] = useState('')
  callback.current = onCredential

  useEffect(() => {
    if (!clientId || !container.current) return
    let cancelled = false
    const render = () => {
      if (cancelled || !window.google?.accounts?.id || !container.current) return
      window.google.accounts.id.initialize({
        client_id: clientId,
        callback: result => { if (result.credential) callback.current(result.credential) },
      })
      container.current.replaceChildren()
      window.google.accounts.id.renderButton(container.current, {
        theme: 'outline', size: 'medium', text: 'continue_with', width: 250,
      })
    }
    if (window.google?.accounts?.id) render()
    else {
      const script = document.createElement('script')
      script.src = 'https://accounts.google.com/gsi/client'
      script.async = true
      script.defer = true
      script.onload = render
      script.onerror = () => { if (!cancelled) setError('Google зараз недоступний') }
      document.head.appendChild(script)
      return () => { cancelled = true; script.remove() }
    }
    return () => { cancelled = true }
  }, [clientId])

  if (!clientId) return null
  return <div aria-busy={disabled}>
    <div ref={container} style={{ opacity: disabled ? 0.5 : 1, pointerEvents: disabled ? 'none' : 'auto' }} />
    {error && <small role="alert">{error}</small>}
  </div>
}
