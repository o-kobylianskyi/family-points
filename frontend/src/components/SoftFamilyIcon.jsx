import { useId } from 'react'

// Product-owned vector iconography. Shared sizes, gradients, and accessible SVG.
// Unlike platform emoji these render consistently across Windows and Android.
export default function SoftFamilyIcon({ name, size = 32, className = '', title = '' }) {
  const id = useId().replaceAll(':', '')
  const gold = `gold-${id}`, mint = `mint-${id}`, aqua = `aqua-${id}`, lilac = `lilac-${id}`
  const gradients = <defs>
    <linearGradient id={gold} x1="0" y1="0" x2="1" y2="1"><stop stopColor="#FFE984"/><stop offset=".55" stopColor="#FFC33E"/><stop offset="1" stopColor="#DF831C"/></linearGradient>
    <linearGradient id={mint} x1="0" y1="0" x2="1" y2="1"><stop stopColor="#A5F7D5"/><stop offset=".5" stopColor="#39C6A5"/><stop offset="1" stopColor="#167C78"/></linearGradient>
    <linearGradient id={aqua} x1="0" y1="0" x2="1" y2="1"><stop stopColor="#C2F9FF"/><stop offset=".6" stopColor="#65BEDA"/><stop offset="1" stopColor="#3385C2"/></linearGradient>
    <linearGradient id={lilac} x1="0" y1="0" x2="1" y2="1"><stop stopColor="#E9D7FF"/><stop offset=".6" stopColor="#B493EC"/><stop offset="1" stopColor="#7E62BD"/></linearGradient>
  </defs>
  const art = ({
    coins: <g stroke="#B8701D" strokeWidth="1.4"><ellipse cx="22" cy="15" rx="13" ry="5.5" fill={`url(#${gold})`}/><path d="M9 15v17c0 3 6 5 13 5s13-2 13-5V15" fill={`url(#${gold})`}/><path d="M9 22c5 5 21 5 26 0M9 29c5 5 21 5 26 0" fill="none" stroke="#FFF5BC"/><ellipse cx="41" cy="36" rx="13" ry="5.5" fill={`url(#${gold})`}/><path d="M28 36v13c0 3 6 5 13 5s13-2 13-5V36" fill={`url(#${gold})`}/><path d="M28 43c5 5 21 5 26 0M28 49c5 5 21 5 26 0" stroke="#FFF0A1" fill="none"/><ellipse cx="41" cy="36" rx="13" ry="5.5" fill={`url(#${gold})`}/></g>,
    star: <g><path d="m32 5 8.1 16.6 18.2 2.7-13.2 12.8 3.1 18.1L32 46.7 15.8 55.2l3.1-18.1L5.7 24.3l18.2-2.7Z" fill={`url(#${gold})`} stroke="#D99421" strokeWidth="2.5" strokeLinejoin="round"/><path d="m32 11 6.4 13.4 14.8 1.8" fill="none" stroke="#FFF5D0" strokeWidth="2.7" strokeLinecap="round"/></g>,
    checklist: <g><rect x="12" y="13" width="40" height="46" rx="7" fill={`url(#${mint})`} stroke="#126B71" strokeWidth="2"/><path d="M25 11h14a4 4 0 0 1 4 4v4H21v-4a4 4 0 0 1 4-4Z" fill="#E8FFF4"/><path d="M22 31l4 4 7-8M22 44l4 4 7-8" stroke="#E8FFF4" strokeWidth="3.3" strokeLinecap="round" strokeLinejoin="round"/><path d="M38 32h8M38 45h8" stroke="#E8FFF4" strokeWidth="3" strokeLinecap="round"/></g>,
    gift: <g><path d="M10 28h44v29H10z" rx="4" fill={`url(#${lilac})`} stroke="#8669BA" strokeWidth="1.5"/><rect x="7" y="23" width="50" height="11" rx="3" fill="#D8C3FB" stroke="#9676C6"/><path d="M28 23c-12 0-16-5-12-10 5-6 13 4 16 10 3-6 11-16 16-10 4 5 0 10-12 10M29 23h6v34h-6Z" fill="#8F70D8" stroke="#7859BD" strokeWidth="2"/><path d="M30 23c-5-10-18-19-18-9 0 7 13 9 18 9Zm4 0c5-10 18-19 18-9 0 7-13 9-18 9Z" fill="#C8A8F9" stroke="#9269CE" strokeWidth="1.5"/></g>,
    crown: <g><path d="m8 23 9 10 13-18 14 18 12-10-5 27H13Z" fill={`url(#${gold})`} stroke="#AC7816" strokeWidth="2" strokeLinejoin="round"/><path d="M14 43h36" stroke="#FFF8BE" strokeWidth="3" strokeLinecap="round"/><circle cx="8" cy="22" r="3.5" fill="#FBEA8B"/><circle cx="30" cy="14" r="3.5" fill="#FBEA8B"/><circle cx="56" cy="22" r="3.5" fill="#FBEA8B"/></g>,
    family: <g><circle cx="23" cy="23" r="9" fill={`url(#${aqua})`}/><circle cx="43" cy="25" r="9" fill={`url(#${mint})`}/><circle cx="32" cy="39" r="7" fill="#FFD4A5"/><path d="M4 54c0-13 9-21 19-21 4 0 8 2 10 6m27 15c0-13-8-21-17-21-5 0-9 2-12 7" stroke="#80C8C9" strokeWidth="8" strokeLinecap="round"/><path d="M20 58c0-8 4-12 12-12s12 4 12 12" stroke="#FFE0B1" strokeWidth="7" strokeLinecap="round"/></g>,
    home: <path d="m9 29 23-20 23 20v25H38V38H26v16H9Z" fill="none" stroke="currentColor" strokeWidth="4" strokeLinejoin="round" strokeLinecap="round"/>,
    points: <g stroke="currentColor" strokeWidth="4" fill="none"><rect x="11" y="31" width="10" height="22" rx="2"/><rect x="27" y="20" width="10" height="33" rx="2"/><rect x="43" y="10" width="10" height="43" rx="2"/></g>,
    check: <g stroke="currentColor" strokeWidth="4" fill="none" strokeLinecap="round" strokeLinejoin="round"><rect x="11" y="9" width="42" height="46" rx="6"/><path d="m21 32 7 7 15-17"/></g>,
    settings: <g fill="none" stroke="currentColor" strokeWidth="4"><circle cx="32" cy="32" r="10"/><path d="m28 6 8 0 2 7 7 3 6-3 6 7-4 6 0 9 4 6-6 7-7-3-6 3-2 7h-8l-2-7-7-3-6 3-6-7 4-6v-9l-4-6 6-7 6 3 7-3Z" strokeLinejoin="round"/></g>,
    heart: <path d="M32 55 8 31C-5 12 19 1 32 20 45 1 69 12 56 31Z" fill={`url(#${mint})`} stroke="#279485" strokeWidth="2"/>,
    plus: <path d="M32 11v42M11 32h42" stroke="currentColor" strokeWidth="5" strokeLinecap="round"/>,
    book: <path d="M7 13c10-4 18-2 25 4 8-6 16-8 25-4v38c-10-4-18-2-25 4-7-6-15-8-25-4Z" fill={`url(#${aqua})`} stroke="#4089B1" strokeWidth="2"/>,
    spark: <path d="m32 4 7 21 21 7-21 7-7 21-7-21-21-7 21-7Z" fill={`url(#${gold})`}/>,
  })[name]
  return <svg width={size} height={size} viewBox="0 0 64 64" fill="none" className={className}
    role={title ? 'img' : undefined} aria-label={title || undefined} aria-hidden={title ? undefined : true}>
    {gradients}{art ?? <circle cx="32" cy="32" r="20" fill={`url(#${mint})`}/>}
  </svg>
}
