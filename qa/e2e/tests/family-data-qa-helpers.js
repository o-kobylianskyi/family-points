import { expect } from '@playwright/test'

// Settings now opens on Backup. Reset preview is mounted only when the Reset tab is active.
// Keep test navigation explicit; never invoke a destructive operation.
export async function openResetPlanning(page) {
  const panel=page.locator('section[aria-label="Family data safety planning"]')
  await expect(panel).toBeVisible({timeout:15000})
  const tabs=panel.locator('button[aria-pressed]')
  await expect(tabs).toHaveCount(3)
  await tabs.nth(1).click()
  await expect(panel.locator('.family-reset-readiness')).toBeVisible({timeout:15000})
  return panel.locator('.family-reset-readiness')
}
export async function openRestorePlanning(page) {
  const panel=page.locator('section[aria-label="Family data safety planning"]')
  await expect(panel).toBeVisible({timeout:15000})
  await panel.locator('button[aria-pressed]').nth(2).click()
  return panel
}
export const backupRow=(workspaceId,snapshotId,fields={})=>({
  workspaceId,snapshotId,state:'REQUESTED',resetMode:'SELECTIVE',
  restoreVerified:false,executable:false,selectedCategories:[],
  createdAt:'2026-11-01T10:00:00Z',expiresAt:'2026-12-01T10:00:00Z',
  ...fields
})
