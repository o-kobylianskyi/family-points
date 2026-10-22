import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import LanguageSwitcher from '../components/LanguageSwitcher'
import './DownloadAppPage.css'

const APK_URL = '/downloads/familypoints-beta.apk'
const MANIFEST_URL = '/downloads/familypoints-beta.json'
const SHA_URL = '/downloads/familypoints-beta.apk.sha256'

export default function DownloadAppPage() {
  const { t } = useTranslation()
  const [release, setRelease] = useState(null)
  const [status, setStatus] = useState('loading')

  useEffect(() => {
    let active = true
    const load = async () => {
      try {
        const [manifestResponse, apkResponse] = await Promise.all([
          fetch(MANIFEST_URL, { cache: 'no-store' }),
          fetch(APK_URL, { method: 'HEAD', cache: 'no-store' }),
        ])
        if (!manifestResponse.ok || !apkResponse.ok) throw new Error('Release not published')
        const manifest = await manifestResponse.json()
        if (
          manifest.file !== 'familypoints-beta.apk' ||
          manifest.applicationId !== 'com.olehkobylianskyi.familypoints.android' ||
          !Number.isInteger(manifest.versionCode) ||
          manifest.versionCode <= 0 ||
          typeof manifest.versionName !== 'string' ||
          !/^[a-f0-9]{64}$/i.test(manifest.sha256) ||
          !Number.isSafeInteger(manifest.sizeBytes) ||
          manifest.sizeBytes <= 0
        ) throw new Error('Invalid release manifest')
        const sizeHeader = apkResponse.headers.get('content-length')
        if (sizeHeader && Number(sizeHeader) !== manifest.sizeBytes) throw new Error('APK size mismatch')
        if (active) {
          setRelease(manifest)
          setStatus('ready')
        }
      } catch {
        if (active) setStatus('unavailable')
      }
    }
    load()
    return () => { active = false }
  }, [])

  return <main className="download-screen">
    <section className="download-panel">
      <div className="download-top"><div className="brand-icon">FP</div><LanguageSwitcher /></div>
      <h1>{t('androidDownload.title')}</h1>
      <p>{t('androidDownload.description')}</p>
      {status === 'ready' && release
        ? <>
            <a className="download-primary" href={APK_URL} download="FamilyPoints-beta.apk">{t('androidDownload.button')}</a>
            <div className="download-release-info">
              <strong>Android BETA {release.versionName}</strong>
              <span>Version code: {release.versionCode} · {(release.sizeBytes / 1024 / 1024).toFixed(1)} MB</span>
              <span>SHA-256:</span>
              <code className="download-hash">{release.sha256}</code>
              <a href={SHA_URL} target="_blank" rel="noreferrer">SHA-256 checksum</a>
            </div>
          </>
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
