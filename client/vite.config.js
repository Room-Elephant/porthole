import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': 'http://localhost:9753',
      '/actuator': 'http://localhost:9753'
    }
  },
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.js'],
    css: true,
    coverage: {
      provider: 'v8',
      reporter: ['text', 'html', 'json-summary', 'json'],
      exclude: [
        'node_modules/',
        'src/test/',
        'src/main.jsx',
        'dist/',
        '*.config.js'
      ],
      thresholds: {
        lines: 70,
        branches: 80,
        functions: 80,
        statements: 80
      }
    }
  }
})
