import { useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { verifyAccountEmail, requestPasswordReset, resetAccountPassword } from '../api/authApi'
import { useTranslation } from 'react-i18next'
import LanguageSwitcher from '../components/LanguageSwitcher'

export default function EmailActionPage({ action }) {
  const { t } = useTranslation()
  const [params] = useSearchParams()
  const token = params.get('token') || ''
  const [email, setEmail] = useState('')
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [pending, setPending] = useState(false)
  const [done, setDone] = useState(false)
  const [error, setError] = useState('')
  const labels = {
    verify: t('emailUi.verify'),
    request: t('emailUi.request'),
    reset: t('emailUi.reset'),
  }
  const submit = async event => {
    event.preventDefault()
    setPending(true)
    setError('')
    try {
      if (action === 'verify') await verifyAccountEmail(token)
      else if (action === 'request') await requestPasswordReset(username, email)
      else await resetAccountPassword(token, password)
      setDone(true)
    } catch (e) {
      setError(e.message || t('emailUi.failed'))
    } finally {
      setPending(false)
    }
  }
  return <div className="login-page"><div className="login-card">
    <LanguageSwitcher />
    <h1>{labels[action]}</h1>
    {done ? <>
      <p>{action === 'request' ? t('emailUi.sent') :
        t('emailUi.done')}</p>
      <Link to="/login">{t("emailUi.toLogin")}</Link>
    </> : <form onSubmit={submit}>
      {action === 'request' && <label>{t("emailUi.username")}
        <input autoComplete="username" required value={username}
          onChange={e => setUsername(e.target.value)} />
      </label>}
      {action === 'request' && <label>Email
        <input type="email" autoComplete="email" required value={email}
          onChange={e => setEmail(e.target.value)} />
      </label>}
      {action === 'reset' && <label>{t('emailUi.passwordNew')}
        <input type="password" autoComplete="new-password" minLength={8} maxLength={128}
          required value={password} onChange={e => setPassword(e.target.value)} />
      </label>}
      {action !== 'request' && !token && <p className="error-message">{t("emailUi.tokenMissing")}</p>}
      {error && <p role="alert" className="error-message">{error}</p>}
      <button type="submit" disabled={pending || (action !== 'request' && !token)}>
        {pending ? t('emailUi.wait') : action === 'verify' ? t('emailUi.confirm') :
          action === 'request' ? t('emailUi.sendLink') : t('emailUi.changePassword')}
      </button>
    </form>}
    <p><Link to="/login">{t("emailUi.return")}</Link></p>
  </div></div>
}
