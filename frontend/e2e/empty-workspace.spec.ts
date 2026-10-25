import { test, expect } from '@playwright/test'
import { PREVIEW_DELAY_MS } from '../src/composables/useLiveExecution'

test('skips empty pipelines while preserving discovery, original text, empty-input steps and whitespace', async ({ page }) => {
 const executions: { input: string; pipeline: unknown[] }[] = []
 let discoveries = 0
 page.on('request', request => {
  const path = new URL(request.url()).pathname
  if (path === '/api/transformations') discoveries++
  if (path === '/api/transform' && request.method() === 'POST') executions.push(request.postDataJSON())
 })
 await page.goto('/')
 await expect(page.getByLabel('Output text')).toHaveText('Real Python!')
 expect(discoveries).toBeGreaterThan(0)
 await page.getByLabel('Find', { exact: true }).fill('')
 await expect(page.getByRole('alert')).toContainText('Find text cannot be empty')
 const requestsBeforeClearing = executions.length
 await page.getByRole('button', { name: 'Clear all', exact: true }).click()
 await expect(page.getByLabel('Output text')).toHaveText('Fake Python')
 await expect(page.locator('.trace')).toHaveCount(0)
 await expect(page.getByRole('alert')).toHaveCount(0)
 await page.getByLabel('Input text').fill('Text without transformations')
 await expect(page.getByLabel('Output text')).toHaveText('Text without transformations')
 await expect(page.getByRole('button', { name: 'Copy', exact: true })).toBeEnabled()
 await page.waitForTimeout(PREVIEW_DELAY_MS * 2)
 expect(executions).toHaveLength(requestsBeforeClearing)
 await page.getByLabel('Input text').fill('')
 const requestsWhenEmpty = executions.length
 await expect(page.locator('.preview-status')).toHaveText('Ready')
 await expect(page.getByLabel('Output text')).toHaveCount(0)
 await expect(page.locator('.trace')).toHaveCount(0)
 await expect(page.getByRole('alert')).toHaveCount(0)
 await expect(page.getByRole('button', { name: 'Copy', exact: true })).toBeDisabled()
 await page.waitForTimeout(PREVIEW_DELAY_MS * 2)
 expect(executions).toHaveLength(requestsWhenEmpty)

 await page.getByRole('button', { name: 'Add Append', exact: true }).click()
 await expect(page.getByLabel('Output text')).toHaveText('!')
 expect(executions.at(-1)?.input).toBe('')
 expect(executions.at(-1)?.pipeline).toHaveLength(1)

 await page.getByRole('button', { name: 'Clear all', exact: true }).click()
 await expect(page.locator('.preview-status')).toHaveText('Ready')
 await page.getByLabel('Input text').fill(' \t\n')
 await expect(page.getByLabel('Output text')).toHaveJSProperty('textContent', ' \t\n')
 await page.waitForTimeout(PREVIEW_DELAY_MS * 2)
 expect(executions).toHaveLength(requestsWhenEmpty + 1)
})
