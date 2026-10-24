// Normalize browser-uploaded images before multipart transmission.
// Keep alpha for PNG avatars. A conservative 850 KiB target stays below
// default 1 MiB reverse-proxy request limits (including multipart overhead).
const TARGET_BYTES = 850 * 1024
const MAX_INPUT_BYTES = 5 * 1024 * 1024
const MAX_SIDE = 768

function toBlob(canvas, mime, quality) {
  return new Promise((resolve, reject) => {
    canvas.toBlob(blob => blob ? resolve(blob) : reject(new Error('Не вдалося оптимізувати зображення. Спробуйте інший файл.')), mime, quality)
  })
}

export async function optimizeMediaUpload(file) {
  if (!file || !['image/png', 'image/jpeg'].includes(file.type) || file.size > MAX_INPUT_BYTES) {
    throw new Error('Підтримуються лише PNG або JPEG розміром до 5 МБ. Оберіть інше зображення.')
  }
  // Avoid changing already-small photos or unnecessarily degrading quality.
  if (file.size <= TARGET_BYTES) return file

  let bitmap
  try {
    bitmap = await createImageBitmap(file)
  } catch {
    throw new Error('Не вдалося прочитати зображення. Спробуйте інший PNG або JPEG.')
  }
  try {
    const longest = Math.max(bitmap.width, bitmap.height)
    for (const side of [MAX_SIDE, 640, 512, 384, 320]) {
      const scale = Math.min(1, side / longest)
      const canvas = document.createElement('canvas')
      canvas.width = Math.max(1, Math.round(bitmap.width * scale))
      canvas.height = Math.max(1, Math.round(bitmap.height * scale))
      const ctx = canvas.getContext('2d')
      if (!ctx) throw new Error('Браузер не зміг підготувати зображення. Спробуйте інший файл.')
      ctx.drawImage(bitmap, 0, 0, canvas.width, canvas.height)
      // Browser PNG encoding preserves transparent pixels.
      // JPEG retains quality with smaller photos that have no alpha channel.
      const mime = file.type
      const blob = await toBlob(canvas, mime, mime === 'image/jpeg' ? 0.82 : undefined)
      if (blob.size <= TARGET_BYTES) {
        return new File([blob], file.name, { type: mime, lastModified: Date.now() })
      }
    }
    throw new Error('Зображення завелике навіть після автоматичного зменшення. Спробуйте файл меншого розміру.')
  } finally {
    bitmap.close()
  }
}
