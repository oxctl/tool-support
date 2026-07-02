import { defineConfig } from '@playwright/test'

export default defineConfig({
  timeout: 60000,
  retries: 2
})
