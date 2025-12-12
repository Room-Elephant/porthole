import { defineConfig, devices } from '@playwright/test';

/**
 * Playwright configuration for Porthole E2E tests.
 * 
 * Prerequisites:
 * - Docker image built: docker build -t porthole:latest .
 * - Test environment running: npm run env:up (or use npm run e2e for full flow)
 */
export default defineConfig({
  testDir: './tests',
  
  // Run tests in parallel
  fullyParallel: true,
  
  // Fail the build on CI if you accidentally left test.only in the source code
  forbidOnly: !!process.env.CI,
  
  // Retry on CI only
  retries: process.env.CI ? 2 : 0,
  
  // Limit parallel workers on CI to avoid flakiness
  workers: process.env.CI ? 1 : undefined,
  
  // Reporter to use
  reporter: [
    ['html', { open: 'never' }],
    ['list']
  ],
  
  // Shared settings for all projects
  use: {
    // Base URL for the Porthole application
    baseURL: 'http://localhost:9753',
    
    // Collect trace when retrying the failed test
    trace: 'on-first-retry',
    
    // Take screenshot on failure
    screenshot: 'only-on-failure',
    
    // Video recording on first retry
    video: 'on-first-retry',
  },

  // Configure projects for major browsers
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
    // Uncomment to add more browser coverage
    // {
    //   name: 'firefox',
    //   use: { ...devices['Desktop Firefox'] },
    // },
    // {
    //   name: 'webkit',
    //   use: { ...devices['Desktop Safari'] },
    // },
  ],

  // Timeout for each test
  timeout: 30_000,
  
  // Timeout for expect() assertions
  expect: {
    timeout: 10_000,
  },

  // Global setup to wait for the application to be ready
  globalSetup: './global-setup.ts',
});

