/** Localize only built-in workspace roles; keep user-created role names intact. */
export function workspaceRoleLabel(t, code, fallback = '') {
  const normalized = String(code || '').toUpperCase()
  const names = {
    FAMILY_ADMIN: { uk: 'Адміністратор сім’ї', de: 'Familienadministrator', en: 'Family administrator', ru: 'Администратор семьи' },
    PARENT: { uk: 'Батьки', de: 'Elternteil', en: 'Parent', ru: 'Родитель' },
    CHILD: { uk: 'Дитина', de: 'Kind', en: 'Child', ru: 'Ребёнок' },
  }
  if (!names[normalized]) return fallback
  return t(`workspaceRoles.${normalized}`, { defaultValue: names[normalized].uk })
}
