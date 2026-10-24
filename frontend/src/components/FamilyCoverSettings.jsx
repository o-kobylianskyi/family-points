import { useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../context/AuthContext'
import './FamilyCoverSettings.css'
import { getMediaManifest, putMediaBinding, deleteMediaBinding, fetchAuthorizedMediaObjectUrl, uploadMedia } from '../api/mediaApi'

const labels = {
  uk: {title:'Обкладинка сім’ї',default:'Стандартна обкладинка',upload:'Завантажити JPEG/PNG',remove:'Відновити стандартну',save:'Обрати',working:'Збереження…',note:'Змінюйте лише для своєї сім’ї'},
  de: {title:'Familien-Titelbild',default:'Standard-Titelbild',upload:'JPEG/PNG hochladen',remove:'Standard wiederherstellen',save:'Auswählen',working:'Speichern…',note:'Nur für Ihre Familie'},
  en: {title:'Family cover',default:'Default cover',upload:'Upload JPEG/PNG',remove:'Restore default',save:'Select',working:'Saving…',note:'For your family only'},
  ru: {title:'Обложка семьи',default:'Стандартная обложка',upload:'Загрузить JPEG/PNG',remove:'Восстановить стандартную',save:'Выбрать',working:'Сохранение…',note:'Только для своей семьи'},
}

export default function FamilyCoverSettings() {
  const { i18n } = useTranslation()
  const t = labels[i18n.language.split('-')[0]] || labels.en
  const { currentUser, getAccessToken } = useAuth()
  const workspaceId = currentUser?.workspaceId
  const canManage = currentUser?.permissions?.some(p => ['MANAGE_WORKSPACE','ADMIN_OVERRIDE'].includes(p))
  const [manifest,setManifest] = useState(null)
  const [coverUrl,setCoverUrl] = useState('')
  const [error,setError] = useState('')
  const [busy,setBusy] = useState(false)
  const [revision,setRevision] = useState(0)
  const requestLock = useRef(false)
  const currentWorkspace = useRef(workspaceId)
  currentWorkspace.current = workspaceId

  useEffect(() => {
    if (!workspaceId) return
    let active=true
    let objectUrl=''
    setManifest(null)
    setCoverUrl('')
    setError('')
    getMediaManifest(getAccessToken(),workspaceId).then(async data => {
      if (!active) return
      setManifest(data)
      if (data.cover?.path) {
        const fetchedUrl=await fetchAuthorizedMediaObjectUrl(getAccessToken(),data.cover.path,workspaceId)
        objectUrl=fetchedUrl
        if (active) setCoverUrl(fetchedUrl)
        else URL.revokeObjectURL(objectUrl)
      }
    }).catch(e=>{if(active)setError(e.message)})
    return () => { active=false; if(objectUrl) URL.revokeObjectURL(objectUrl) }
  },[workspaceId,getAccessToken,revision])

  const mutate = async action => {
    if (requestLock.current || !manifest) return
    const selectedWorkspace = workspaceId
    requestLock.current = true
    setBusy(true);setError('')
    try { await action(); if(currentWorkspace.current===selectedWorkspace)setRevision(n=>n+1) }
    catch (e) {
      if(currentWorkspace.current===selectedWorkspace){setError(e.message);if(e.status===409)setRevision(n=>n+1)}
    } finally {requestLock.current=false;if(currentWorkspace.current===selectedWorkspace)setBusy(false)}
  }
  const cover=manifest?.cover
  const labelForKey = key => {
    const names = {
      'family-default': {uk:'Сімейна ілюстрація',de:'Familienillustration',en:'Family illustration',ru:'Семейная иллюстрация'},
      'family-cover-default': {uk:'Стандартна сімейна',de:'Standard-Familie',en:'Default family',ru:'Стандартная семейная'},
    }
    const lang = i18n.language.split('-')[0]
    return names[key]?.[lang] || names[key]?.en || key.replace(/[-_]/g, ' ')
  }
  const choose = key => mutate(()=>putMediaBinding(getAccessToken(),workspaceId,'WORKSPACE',workspaceId,'COVER',{catalogKey:key,assetId:null},cover.version))
  const reset = () => mutate(()=>deleteMediaBinding(getAccessToken(),workspaceId,'WORKSPACE',workspaceId,'COVER',cover.version))
  const upload = file => {
    if (!file) return
    if (!['image/jpeg','image/png'].includes(file.type) || file.size>5*1024*1024) {
      setError('JPEG/PNG · max 5 MiB')
      return
    }
    return mutate(async()=>{
      const asset = await uploadMedia(getAccessToken(),workspaceId,file)
      const id=asset.assetId || asset.id
      if (!id) throw new Error('Upload did not return asset ID')
      await putMediaBinding(getAccessToken(),workspaceId,'WORKSPACE',workspaceId,'COVER',{assetId:id,catalogKey:null},cover.version)
    })
  }
  return <section className="settings-section family-cover-settings">
    <h2>{t.title}</h2>
    <p>{t.note}</p>
    {error&&<p role="alert" className="error-message">{error}</p>}
    <div className="family-cover-layout">
      <img className="family-cover-preview" src={coverUrl||'/assets/soft-family/reference/hero-family.webp'} alt={t.title}/>
      {canManage&&cover&&<div className="family-cover-choices">
        <strong>{t.save}</strong>
        <div className="family-cover-choice-list">
          {(manifest.coverChoices||[]).filter(item=>item.selectable&&item.previewPath).map(item=>
            <button type="button" className={'family-cover-choice'+(cover.catalogKey===item.key&&cover.source!=='DEFAULT'?' selected':'')}
              key={item.key} disabled={busy} onClick={()=>choose(item.key)}>
              <span className="family-cover-choice-symbol" aria-hidden="true">▧</span>
              <span>{labelForKey(item.key)}</span>
            </button>)}
        </div>
      </div>}
    </div>
    <p>{cover?.source==='DEFAULT'?t.default:labelForKey(cover?.catalogKey||'')}</p>
    {canManage&&cover&&<div className="family-cover-actions">
      <label className={'primary-button family-cover-upload'+(busy?' disabled':'')}>
        {t.upload}
        <input type="file" accept="image/jpeg,image/png" disabled={busy}
          onChange={e=>{const file=e.target.files?.[0];e.target.value='';upload(file)}}/>
      </label>
      <button type="button" className="secondary-button" disabled={busy||cover.source==='DEFAULT'} onClick={reset}>{t.remove}</button>
      {busy&&<small role="status">{t.working}</small>}
    </div>}
  </section>
}
