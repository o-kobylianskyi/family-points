import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import LanguageSwitcher from '../components/LanguageSwitcher'
import './DownloadAppPage.css'

const APK_URL = '/downloads/familypoints-beta.apk'

export default function DownloadAppPage() {
  const { t } = useTranslation()
  const [available, setAvailable] = useState(false)
  useEffect(() => {
    let active = true
    fetch(APK_URL, { method: 'HEAD', cache: 'no-store' })
      .then(response => { if (active) setAvailable(response.ok && response.headers.get('content-type')?.includes('android.package-archive')) })
      .catch(() => { if (active) setAvailable(false) })
    return () => { active = false }
  }, [])
  return <main className="download-screen">
    <section className="download-panel">
      <div className="download-top"><div className="brand-icon">FP</div><LanguageSwitcher /></div>
      <h1>{t('androidDownload.title')}</h1>
      <p>{t('androidDownload.description')}</p>
      {available
        ? <a className="download-primary" href={APK_URL} download="FamilyPoints-beta.apk">{t('androidDownload.button')}</a>
        : <p className="download-unavailable" role="status">{t('androidDownload.unavailable')}</p>}
      <h2>{t('androidDownload.installTitle')}</h2>
      <ol>
        <li>{t('androidDownload.stepOne')}</li>
        <li>{t('androidDownload.stepTwo')}</li>
        <li>{t('androidDownload.stepThree')}</li>
      </ol>
      <p className="download-note">{t('androidDownload.updateNote')}</p>
      <Link to="/login">{t('androidDownload.login')}</Link>
    </section>
  </main>
}
