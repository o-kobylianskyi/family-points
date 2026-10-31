// Display-only labels for bundled catalog category codes. Never translate API identifiers.
const labels = {
  activity: ['Активність','Активность','Aktivität','Activity'],
  adult: ['Завдання дорослих','Задачи взрослых','Aufgaben für Erwachsene','Adult tasks'],
  chores: ['Домашні справи','Домашние дела','Hausarbeit','Chores'],
  creativity: ['Творчість','Творчество','Kreativität','Creativity'],
  family: ['Сім’я','Семья','Familie','Family'],
  habits: ['Корисні звички','Полезные привычки','Gewohnheiten','Habits'],
  hygiene: ['Гігієна','Гигиена','Hygiene','Hygiene'],
  reading: ['Читання','Чтение','Lesen','Reading'],
  routine: ['Щоденний розпорядок','Распорядок дня','Tagesablauf','Daily routine'],
  school: ['Навчання','Учёба','Schule','School'],
  social: ['Спілкування','Общение','Soziales','Social'],
  time: ['Екранний час','Экранное время','Bildschirmzeit','Screen time'],
  privilege: ['Привілеї','Привилегии','Privilegien','Privileges'],
  items: ['Речові нагороди','Вещевые награды','Sachbelohnungen','Material rewards'],
  other: ['Інше','Другое','Sonstiges','Other'],
}
const indices = { uk: 0, ru: 1, de: 2, en: 3 }
export function templateCategoryLabel(code, locale = 'en') {
  const lang = String(locale).split('-')[0]
  return labels[code]?.[indices[lang] ?? 3] || code
}
