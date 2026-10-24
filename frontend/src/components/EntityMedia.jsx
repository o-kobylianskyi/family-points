import { createPortal } from 'react-dom'
import { useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../context/AuthContext'
import './EntityMedia.css'
import { getMediaBinding, getMediaCatalog, putMediaBinding, deleteMediaBinding, uploadMedia, fetchAuthorizedMediaObjectUrl } from '../api/mediaApi'

const dictionary = {
  uk: ['Зображення','Обрати','Завантажити JPEG/PNG','Скинути','Зберігається…','Немає зображення'],
  de: ['Bild','Auswählen','JPEG/PNG hochladen','Zurücksetzen','Speichert…','Kein Bild'],
  en: ['Image','Choose','Upload JPEG/PNG','Reset','Saving…','No image'],
  ru: ['Изображение','Выбрать','Загрузить JPEG/PNG','Сбросить','Сохранение…','Нет изображения'],
}
export default function EntityMedia({ type, targetId, slot='ICON', canEdit=false, compact=false, inlineEditor=false, children=null }) {
  const {currentUser,getAccessToken}=useAuth()
  const {i18n}=useTranslation()
  const words=dictionary[i18n.language.split('-')[0]]||dictionary.en
  const workspaceId=currentUser.workspaceId
  const [reference,setReference]=useState(null)
  const [catalog,setCatalog]=useState([])
  const [url,setUrl]=useState('')
  const [error,setError]=useState('')
  const [busy,setBusy]=useState(false)
  const [editorOpen,setEditorOpen]=useState(false)
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
    try{await action();if(activeTarget.current===targetKey){
      setRevision(v=>v+1)
      if(type==='MEMBER')window.dispatchEvent(new CustomEvent('fp:member-avatar-changed',{detail:{workspaceId,memberId:targetId}}))
    }}
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
  const locale=i18n.language.split('-')[0]
  const labels={
    memberDefault:['Стандартний аватар','Standardavatar','Default avatar','Стандартный аватар'],
    avatarSunflower:['Соняшник','Sonnenblume','Sunflower','Подсолнух'],
    avatarSky:['Небо','Himmel','Sky','Небо'],
    avatarBerry:['Ягода','Beere','Berry','Ягода'],
    avatarMint:['М’ята','Minze','Mint','Мята'],
    avatarPeach:['Персик','Pfirsich','Peach','Персик'],
    avatarViolet:['Фіалка','Veilchen','Violet','Фиалка'],
    avatarOcean:['Океан','Ozean','Ocean','Океан'],
    avatarForest:['Ліс','Wald','Forest','Лес'],
    peopleGroupDefault:['Група людей','Personengruppe','People group','Группа людей'],
    taskGroupDefault:['Група завдань','Aufgabengruppe','Task group','Группа заданий'],
    taskDefault:['Завдання','Aufgabe','Task','Задание'],
  }
  const languageIndex={uk:0,de:1,en:2,ru:3}[locale]??2
  const catalogName=item=>{
    const cleanKey=(item.labelKey||'').split('.').pop()
    return labels[cleanKey]?.[languageIndex]||
      item.key.replace(/^(avatar|member|task|peopleGroup)[-_]?/i,'').replace(/[-_]/g,' ')||
      words[0]
  }
  const allowedCategories=type==='MEMBER'?['AVATAR']:type==='MEMBER_GROUP'?['GROUP']:type==='TASK'?['TASK','TASK_CATEGORY']:['FAMILY']
  const availableCatalog=catalog.filter(item=>
    item.selectable && item.previewPath &&
    allowedCategories.includes(String(item.category||'').toUpperCase())
  )
  const editor = canEdit && reference && <div className="entity-media-controls">
    <select aria-label={words[1]} value="" disabled={busy} onChange={e=>{if(e.target.value)choose(e.target.value)}}>
      <option value="">{words[1]}</option>
      {availableCatalog.map(item=><option key={item.key} value={item.key}>{catalogName(item)}</option>)}
    </select>
    <label className="entity-media-upload">{words[2]}<input type="file" accept="image/png,image/jpeg" disabled={busy} onChange={e=>{upload(e.target.files?.[0]);e.target.value=''}}/></label>
    <button type="button" className="secondary-button" disabled={busy||reference.source==='DEFAULT'} onClick={clear}>{words[3]}</button>
  </div>
  if(inlineEditor) return <div className="member-avatar-editor">
    <div className="member-avatar-editor-top">
      <div className="entity-media-avatar">{url?<img src={url} alt={words[0]}/>:children}</div>
      <button type="button" className="secondary-button member-avatar-change" disabled={!canEdit} onClick={()=>setEditorOpen(v=>!v)}>
        {({uk:'Змінити',de:'Ändern',en:'Change',ru:'Изменить'})[locale]||'Change'}
      </button>
    </div>
    {editorOpen&&<div className="member-avatar-editor-panel">{editor}{busy&&<small>{words[4]}</small>}{error&&<small role="alert">{error}</small>}</div>}
  </div>
  if(compact) return <div className="entity-media-compact" onClick={e=>e.stopPropagation()}>
    <div className="entity-media-avatar">{url?<img src={url} alt={words[0]}/>:children}</div>
    {canEdit&&<button type="button" className="entity-media-edit" aria-label={words[1]} title={words[1]} onClick={()=>setEditorOpen(true)}>✎</button>}
    {editorOpen&&createPortal(<div className="entity-media-backdrop" role="presentation" onClick={()=>{if(!busy)setEditorOpen(false)}}>
      <section role="dialog" aria-modal="true" aria-label={words[0]} className="entity-media-dialog" onClick={e=>e.stopPropagation()}>
        <div className="entity-media-dialog-heading"><strong>{words[0]}</strong><button type="button" className="secondary-button" disabled={busy} onClick={()=>setEditorOpen(false)}>×</button></div>
        {url?<img src={url} alt={words[0]} className="entity-media-preview"/>:<div className="entity-media-preview-placeholder">{children}</div>}
        {editor}
        {busy&&<small>{words[4]}</small>}
        {error&&<small role="alert">{error}</small>}
      </section>
    </div>, document.body)}
  </div>
  return <section className="entity-media">
    {url&&<img src={url} alt={words[0]} className="entity-media-preview-small"/>}
    {editor}
    {busy&&<small>{words[4]}</small>}
    {error&&<small role="alert">{error}</small>}
  </section>
}
