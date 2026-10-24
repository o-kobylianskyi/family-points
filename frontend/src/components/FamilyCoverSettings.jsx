import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../context/AuthContext'
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
    if (busy || !manifest) return
    setBusy(true);setError('')
    try { await action(); setRevision(n=>n+1) }
    catch (e) {setError(e.message)}
    finally {setBusy(false)}
  }
  const cover=manifest?.cover
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
  return <section className="settings-section">
    <h2>{t.title}</h2>
    <p>{t.note}</p>
    {error&&<p role="alert" className="error-message">{error}</p>}
    <img src={coverUrl||'/assets/soft-family/reference/hero-family.webp'} alt={t.title} style={{width:'min(100%,450px)',height:'auto',borderRadius:12,display:'block',marginBottom:12}} />
    <p>{cover?.source==='DEFAULT'?t.default:cover?.catalogKey||t.title}</p>
    {canManage&&cover&&<>
      <div style={{display:'flex',flexWrap:'wrap',gap:10,marginBottom:12}}>
        {(manifest.coverChoices||[]).filter(c=>c.selectable).map(item=><button type="button" className="secondary-button" key={item.key}
          disabled={busy} onClick={()=>choose(item.key)}>{t.save}: {item.labelKey||item.key}</button>)}
        <button type="button" className="secondary-button" disabled={busy||cover.source==='DEFAULT'} onClick={reset}>{t.remove}</button>
      </div>
      <label>{t.upload}<input type="file" accept="image/jpeg,image/png" disabled={busy} onChange={e=>{const file=e.target.files?.[0];e.target.value='';upload(file)}} /></label>
      {busy&&<small>{t.working}</small>}
    </>}
  </section>
}
