import { useEffect, useState } from 'react'
import { fetchAuthorizedMediaObjectUrl } from '../api/mediaApi'

// Each item owns and revokes its authenticated preview URL.
export default function AvatarCatalogTile({ item, workspaceId, token, selected, onSelect, label }) {
  const [preview, setPreview] = useState('')
  useEffect(() => {
    let active = true
    let localUrl = ''
    setPreview('')
    const rawPath = item.previewPath?.replaceAll('{workspaceId}', String(workspaceId))
    if (!rawPath || !token) return () => { active = false }
    fetchAuthorizedMediaObjectUrl(token, rawPath, workspaceId).then(url => {
      localUrl = url
      if (active) setPreview(url)
      else URL.revokeObjectURL(url)
    }).catch(() => { if (active) setPreview('') })
    return () => { active = false; if (localUrl) URL.revokeObjectURL(localUrl) }
  }, [item.previewPath, workspaceId, token])

  return <button type="button" className={'avatar-catalog-tile' + (selected ? ' selected' : '')}
    aria-label={label} aria-pressed={selected} title={label} onClick={() => onSelect(item.key)}>
    {preview ? <img src={preview} alt="" loading="lazy" /> : <span className="avatar-catalog-fallback" aria-hidden="true">◯</span>}
    {selected && <span className="avatar-catalog-check" aria-hidden="true">✓</span>}
  </button>
}
