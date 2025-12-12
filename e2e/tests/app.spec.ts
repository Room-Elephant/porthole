import { test, expect } from '@playwright/test';

test.describe('Porthole E2E Tests', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
  });

  test.describe('Application Health', () => {
    test('should load the application successfully', async ({ page }) => {
      // Verify the main app loads
      await expect(page.locator('h1')).toHaveText('Porthole');
      
      // Verify the settings button is visible
      await expect(page.locator('.settings-btn')).toBeVisible();
    });

    test('should show Docker status as connected in settings', async ({ page }) => {
      // Open settings
      await page.locator('.settings-btn').click();
      
      // Wait for the settings modal
      await expect(page.locator('.app-settings-modal')).toBeVisible();
      
      // Check that Docker status shows as connected
      await expect(page.locator('.status-label')).toContainText('Docker: Connected');
    });
  });

  test.describe('Container List', () => {
    test('should display running containers', async ({ page }) => {
      // Wait for containers to load (skeleton should disappear)
      await expect(page.locator('.container-grid .card').first()).toBeVisible({ timeout: 10000 });
      
      // Verify at least one container is displayed
      const containers = page.locator('.container-grid .card');
      await expect(containers).toHaveCount(await containers.count());
      expect(await containers.count()).toBeGreaterThan(0);
    });

    test('should display the mock nginx container', async ({ page }) => {
      // Wait for containers to load
      await expect(page.locator('.container-grid .card').first()).toBeVisible({ timeout: 10000 });
      
      // Look for the mock-nginx-e2e container
      const nginxContainer = page.locator('.card', { hasText: 'mock-nginx-e2e' });
      await expect(nginxContainer).toBeVisible();
      
      // Verify it shows the nginx image
      await expect(nginxContainer.locator('.container-image')).toContainText('nginx');
    });

    test('should show container status indicator', async ({ page }) => {
      // Wait for containers to load
      await expect(page.locator('.container-grid .card').first()).toBeVisible({ timeout: 10000 });
      
      // Verify status indicators are present on container cards
      const statusIndicators = page.locator('.card .status-indicator');
      expect(await statusIndicators.count()).toBeGreaterThan(0);
    });
  });

  test.describe('Container Settings', () => {
    test('should open container settings when clicking the settings icon', async ({ page }) => {
      // Wait for containers to load
      await expect(page.locator('.container-grid .card').first()).toBeVisible({ timeout: 10000 });
      
      // Click the settings button on the first container
      await page.locator('.card .config-btn').first().click();
      
      // Verify settings modal opens
      await expect(page.locator('.container-settings-modal, .modal')).toBeVisible();
    });
  });

  test.describe('Filtering', () => {
    test('should toggle "Show stopped containers" setting', async ({ page }) => {
      // Open settings
      await page.locator('.settings-btn').click();
      await expect(page.locator('.app-settings-modal')).toBeVisible();
      
      // Find the "Show stopped containers" toggle
      const stoppedToggle = page.locator('.toggle-switch', { hasText: 'Show stopped containers' });
      await expect(stoppedToggle).toBeVisible();
      
      // Get initial state
      const checkbox = stoppedToggle.locator('input[type="checkbox"]');
      const initialState = await checkbox.isChecked();
      
      // Toggle the setting
      await checkbox.click();
      
      // Verify the state changed
      expect(await checkbox.isChecked()).toBe(!initialState);
      
      // Close modal
      await page.keyboard.press('Escape');
    });

    test('should toggle "Show containers without ports" setting', async ({ page }) => {
      // Open settings
      await page.locator('.settings-btn').click();
      await expect(page.locator('.app-settings-modal')).toBeVisible();
      
      // Find the "Show containers without ports" toggle
      const portsToggle = page.locator('.toggle-switch', { hasText: 'Show containers without ports' });
      await expect(portsToggle).toBeVisible();
      
      // Get initial state
      const checkbox = portsToggle.locator('input[type="checkbox"]');
      const initialState = await checkbox.isChecked();
      
      // Toggle the setting
      await checkbox.click();
      
      // Verify the state changed
      expect(await checkbox.isChecked()).toBe(!initialState);
    });

    test('should show additional containers when enabling "Show containers without ports"', async ({ page }) => {
      // Wait for initial containers to load
      await expect(page.locator('.container-grid .card').first()).toBeVisible({ timeout: 10000 });
      
      // Count initial containers
      const initialCount = await page.locator('.card').count();
      
      // Open settings and enable "Show containers without ports"
      await page.locator('.settings-btn').click();
      await expect(page.locator('.app-settings-modal')).toBeVisible();
      
      const portsToggle = page.locator('.toggle-switch', { hasText: 'Show containers without ports' });
      const checkbox = portsToggle.locator('input[type="checkbox"]');
      
      // Only toggle if not already checked
      if (!(await checkbox.isChecked())) {
        await checkbox.click();
      }
      
      // Close modal
      await page.keyboard.press('Escape');
      
      // Wait for containers to reload
      await page.waitForResponse(response => 
        response.url().includes('/api/containers') && response.status() === 200
      );
      
      // The mock-alpine-e2e container should now be visible (no ports)
      await expect(page.locator('.card', { hasText: 'mock-alpine-e2e' })).toBeVisible({ timeout: 10000 });
    });
  });

  test.describe('UI Elements', () => {
    test('should display container icons', async ({ page }) => {
      // Wait for containers to load
      await expect(page.locator('.container-grid .card').first()).toBeVisible({ timeout: 10000 });
      
      // Verify container icons are present
      const icons = page.locator('.card .container-icon');
      expect(await icons.count()).toBeGreaterThan(0);
      
      // Verify icons have src attribute
      const firstIcon = icons.first();
      await expect(firstIcon).toHaveAttribute('src', /.+/);
    });

    test('should display container names and images', async ({ page }) => {
      // Wait for containers to load
      await expect(page.locator('.container-grid .card').first()).toBeVisible({ timeout: 10000 });
      
      // Verify container info is displayed
      const containerInfo = page.locator('.card .container-info').first();
      await expect(containerInfo.locator('h3')).toBeVisible();
      await expect(containerInfo.locator('.container-image')).toBeVisible();
    });
  });
});

