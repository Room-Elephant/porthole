/**
 * Global setup for Playwright E2E tests.
 * Waits for the Porthole application to be healthy before running tests.
 */

const BASE_URL = 'http://localhost:9753';
const HEALTH_ENDPOINT = `${BASE_URL}/actuator/health`;
const MAX_RETRIES = 30;
const RETRY_DELAY_MS = 2000;

async function waitForHealth(): Promise<void> {
  console.log('Waiting for Porthole to be healthy...');
  
  for (let attempt = 1; attempt <= MAX_RETRIES; attempt++) {
    try {
      const response = await fetch(HEALTH_ENDPOINT);
      if (response.ok) {
        const data = await response.json();
        if (data.status === 'UP') {
          console.log(`Porthole is healthy (attempt ${attempt}/${MAX_RETRIES})`);
          return;
        }
      }
    } catch {
      // Connection refused or other error, keep trying
    }
    
    if (attempt < MAX_RETRIES) {
      console.log(`Attempt ${attempt}/${MAX_RETRIES} failed, retrying in ${RETRY_DELAY_MS}ms...`);
      await new Promise(resolve => setTimeout(resolve, RETRY_DELAY_MS));
    }
  }
  
  throw new Error(`Porthole did not become healthy after ${MAX_RETRIES} attempts. Make sure the test environment is running: npm run env:up`);
}

export default async function globalSetup(): Promise<void> {
  await waitForHealth();
}

