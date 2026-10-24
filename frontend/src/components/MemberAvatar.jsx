import { useEffect, useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { getMediaBinding, fetchAuthorizedMediaObjectUrl } from '../api/mediaApi'
import FamilyAvatar from './FamilyAvatar'

// Reads the effective member avatar through the authorized Media API.
// A transient Blob URL is scoped to the current account/workspace/member and revoked on cleanup.
export default function MemberAvatar({ memberId, name = '', className = '', size = 'normal', title }) {
  const { currentUser, getAccessToken } = useAuth()
  const workspaceId = currentUser?.workspaceId
  const accountId = currentUser?.accountId || currentUser?.userId || currentUser?.memberId
  const [imageUrl, setImageUrl] = useState(null)

  useEffect(() => {
    let active = true
    let createdUrl = null
    setImageUrl(null)
    if (!workspaceId || !memberId) return () => { active = false }
    const token = getAccessToken()
    getMediaBinding(token, workspaceId, 'MEMBER', memberId, 'AVATAR')
      .then(ref => ref?.path && ref.source !== 'DEFAULT'
        ? fetchAuthorizedMediaObjectUrl(token, ref.path, workspaceId)
        : null)
      .then(url => {
        if (!url) return
        createdUrl = url
        if (active) setImageUrl(url)
        else URL.revokeObjectURL(url)
      })
      .catch(() => { if (active) setImageUrl(null) })
    return () => {
      active = false
      if (createdUrl) URL.revokeObjectURL(createdUrl)
    }
  }, [accountId, workspaceId, memberId, getAccessToken])

  return <FamilyAvatar name={name} imageUrl={imageUrl} kind="member" className={className} size={size} title={title} />
}
