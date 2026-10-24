import { useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../context/AuthContext'
import { getMediaBinding, getMediaCatalog, putMediaBinding, deleteMediaBinding, uploadMedia, fetchAuthorizedMediaObjectUrl } from '../api/mediaApi'

const dictionary = {
  uk: ['Зображення','Обрати','Завантажити JPEG/PNG','Скинути','Зберігається…','Немає зображення'],
  de: ['Bild','Auswählen','JPEG/PNG hochladen','Zurücksetzen','Speichert…','Kein Bild'],
  en: ['Image','Choose','Upload JPEG/PNG','Reset','Saving…','No image'],
  ru: ['Изображение','Выбрать','Загрузить JPEG/PNG','Сбросить','Сохранение…','Нет изображения'],
}
export default function EntityMedia({ type, targetId, slot='ICON', canEdit=false }) {
  const {currentUser,getAccessToken}=useAuth()
  const {i18n}=useTranslation()
  const words=dictionary[i18n.language.split('-')[0]]||dictionary.en
  const workspaceId=currentUser.workspaceId
  const [reference,setReference]=useState(null)
  const [catalog,setCatalog]=useState([])
  const [url,setUrl]=useState('')
  const [error,setError]=useState('')
  const [busy,setBusy]=useState(false)
  const [revision,setRevision]=useState(0)
  const lock=useRef(false)
  const activeTarget=useRef('')
  activeTarget.current=`${workspaceId}:${type}:${targetId}:${slot}`
  useEffect(()=>{
    if(!targetId)return
    let alive=true
    let objectUrl=''
    setReference(null);setUrl('');setError('');setCatalog([]);setBusy(false)
    Promise.all([getMediaBinding(getAccessToken(),workspaceId,type,targetId,slot),
      canEdit?getMediaCatalog(getAccessToken(),workspaceId):Promise.resolve([])])
      .then(async ([ref,items])=>{
        if(!alive)return
        setReference(ref);setCatalog(Array.isArray(items)?items:[])
        if(ref?.path&&ref.source!=='DEFAULT'){
          const resolved=await fetchAuthorizedMediaObjectUrl(getAccessToken(),ref.path,workspaceId)
          objectUrl=resolved
          if(alive)setUrl(resolved)
          else URL.revokeObjectURL(resolved)
        }
      }).catch(e=>{if(alive)setError(e.message)})
    return ()=>{alive=false;if(objectUrl)URL.revokeObjectURL(objectUrl)}
  },[workspaceId,type,targetId,slot,canEdit,getAccessToken,revision])
  const mutate=async action=>{
    if(lock.current||!reference)return
    const targetKey=activeTarget.current
    lock.current=true;setBusy(true);setError('')
    try{await action();if(activeTarget.current===targetKey)setRevision(v=>v+1)}
    catch(e){if(activeTarget.current===targetKey){setError(e.message);if(e.status===409)setRevision(v=>v+1)}}
    finally{lock.current=false;if(activeTarget.current===targetKey)setBusy(false)}
  }
  const choose=key=>mutate(()=>putMediaBinding(getAccessToken(),workspaceId,type,targetId,slot,{catalogKey:key,assetId:null},reference.version))
  const clear=()=>mutate(()=>deleteMediaBinding(getAccessToken(),workspaceId,type,targetId,slot,reference.version))
  const upload=file=>{
    if(!file)return
    if(!['image/png','image/jpeg'].includes(file.type)||file.size>5*1024*1024){setError('JPEG/PNG · max 5 MiB');return}
    mutate(async()=>{
      const data=await uploadMedia(getAccessToken(),workspaceId,file)
      const assetId=data.assetId||data.id
      if(!assetId)throw new Error('Missing assetId')
      await putMediaBinding(getAccessToken(),workspaceId,type,targetId,slot,{assetId,catalogKey:null},reference.version)
    })
  }
  return <section className="entity-media" style={{display:'flex',alignItems:'center',gap:12,flexWrap:'wrap'}}>
    {url&&<img src={url} alt={words[0]} style={{width:64,height:64,objectFit:'cover',borderRadius:12}}/>}
    {!url&&<small>{words[5]}</small>}
    {canEdit&&reference&&<div style={{display:'flex',gap:8,flexWrap:'wrap'}}>
      <select aria-label={words[1]} value="" disabled={busy} onChange={e=>{if(e.target.value)choose(e.target.value)}}>
        <option value="">{words[1]}</option>
        {catalog.filter(x=>x.selectable&&x.previewPath).map(item=><option key={item.key} value={item.key}>{item.labelKey||item.key}</option>)}
      </select>
      <label>{words[2]}<input type="file" accept="image/png,image/jpeg" disabled={busy} onChange={e=>{upload(e.target.files?.[0]);e.target.value=''}}/></label>
      <button type="button" className="secondary-button" disabled={busy||reference.source==='DEFAULT'} onClick={clear}>{words[3]}</button>
    </div>}
    {busy&&<small>{words[4]}</small>}
    {error&&<small role="alert">{error}</small>}
  </section>
}
