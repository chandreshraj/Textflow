import { defineConfig } from '@playwright/test'
export default defineConfig({ timeout: 120000, testDir: './e2e', use: { baseURL: 'http://127.0.0.1:8080', headless: true }, webServer: { command: 'java -jar ../backend/web-api/target/web-api-1.0.0.jar', url: 'http://127.0.0.1:8080/api/transformations', reuseExistingServer: !process.env.CI } })
