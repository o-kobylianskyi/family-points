import { useState } from 'react'
import SoftFamilyIcon from './SoftFamilyIcon'

// Presentation only. Private media may arrive as a revocable Blob URL from the authenticated Media API.
// Image sources must be supplied by trusted components, never inferred from emails.
export default function FamilyAvatar({ name = '', imageUrl = null, kind = 'member', className = '', size = 'normal', title }) {
  const [failedUrl, setFailedUrl] = useState(null)
  const fallback = Array.from(String(name).trim())[0]?.toLocaleUpperCase() || '?'
  const showImage = typeof imageUrl === 'string'
    && (imageUrl.startsWith('/') || imageUrl.startsWith('https://') || imageUrl.startsWith('blob:'))
    && failedUrl !== imageUrl
  const label = title || name || (kind === 'family' ? 'Family' : 'Member')
  return <span
    className={['fp-avatar', 'fp-avatar--' + kind, 'fp-avatar--' + size, className].filter(Boolean).join(' ')}
    role="img" aria-label={label} title={label}
  >
    {showImage
      ? <img src={imageUrl} alt="" loading="lazy" onError={() => setFailedUrl(imageUrl)} />
      : kind === 'family' || kind === 'group'
        ? <SoftFamilyIcon name="family" size={size === 'large' ? 58 : 32} />
        : <span className="fp-avatar-initial">{fallback}</span>}
  </span>
}
