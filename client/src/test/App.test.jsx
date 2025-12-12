import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor, fireEvent } from '@testing-library/react';
import App from '../App';
import { renderWithQuery, createMockContainer } from './testUtils';
import * as useContainersHook from '../hooks/useContainers';
import * as useDockerHealthHook from '../hooks/useDockerHealth';
import * as useLocalStorageHook from '../hooks/useLocalStorage';

vi.mock('../hooks/useContainers');
vi.mock('../hooks/useDockerHealth');
vi.mock('../hooks/useLocalStorage');

describe('App', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    
    useLocalStorageHook.useLocalStorage.mockImplementation((key, defaultValue) => {
      return [defaultValue, vi.fn()];
    });

    useDockerHealthHook.useDockerHealth.mockReturnValue({
      data: { status: 'UP' },
      isLoading: false,
      isError: false,
    });
  });

  describe('loading state', () => {
    it('shows skeleton tiles while loading', () => {
      useContainersHook.useContainers.mockReturnValue({
        data: [],
        isLoading: true,
        error: null,
        refetch: vi.fn(),
      });

      renderWithQuery(<App />);

      const skeletons = document.querySelectorAll('.skeleton-card');
      expect(skeletons.length).toBe(6);
    });
  });

  describe('error state', () => {
    it('shows error message when API fails', () => {
      useContainersHook.useContainers.mockReturnValue({
        data: [],
        isLoading: false,
        error: new Error('API Error'),
        refetch: vi.fn(),
      });

      renderWithQuery(<App />);

      expect(screen.getByText(/Failed to fetch containers/)).toBeInTheDocument();
    });

    it('shows Docker unavailable when 502 error', () => {
      useContainersHook.useContainers.mockReturnValue({
        data: [],
        isLoading: false,
        error: { response: { status: 502 } },
        refetch: vi.fn(),
      });

      renderWithQuery(<App />);

      expect(screen.getByText(/Docker unavailable/)).toBeInTheDocument();
    });
  });

  describe('empty state', () => {
    it('shows empty state when no containers', () => {
      useContainersHook.useContainers.mockReturnValue({
        data: [],
        isLoading: false,
        error: null,
        refetch: vi.fn(),
      });

      renderWithQuery(<App />);

      expect(screen.getByText(/No containers found/)).toBeInTheDocument();
    });
  });

  describe('containers display', () => {
    it('renders standalone containers', () => {
      const containers = [
        createMockContainer({ id: '1', name: 'container-1', displayName: 'Container 1', project: null }),
        createMockContainer({ id: '2', name: 'container-2', displayName: 'Container 2', project: null }),
      ];

      useContainersHook.useContainers.mockReturnValue({
        data: containers,
        isLoading: false,
        error: null,
        refetch: vi.fn(),
      });

      renderWithQuery(<App />);

      expect(screen.getByText('Container 1')).toBeInTheDocument();
      expect(screen.getByText('Container 2')).toBeInTheDocument();
    });

    it('groups containers by project', () => {
      const containers = [
        createMockContainer({ id: '1', name: 'app', displayName: 'App', project: 'my-project' }),
        createMockContainer({ id: '2', name: 'db', displayName: 'Database', project: 'my-project' }),
      ];

      useContainersHook.useContainers.mockReturnValue({
        data: containers,
        isLoading: false,
        error: null,
        refetch: vi.fn(),
      });

      renderWithQuery(<App />);

      expect(screen.getByText('my-project')).toBeInTheDocument();
      expect(screen.getByText('App')).toBeInTheDocument();
      expect(screen.getByText('Database')).toBeInTheDocument();
    });
  });

  describe('settings', () => {
    it('opens settings modal when clicking settings button', async () => {
      useContainersHook.useContainers.mockReturnValue({
        data: [],
        isLoading: false,
        error: null,
        refetch: vi.fn(),
      });

      renderWithQuery(<App />);

      const settingsButton = screen.getByTitle('Settings');
      fireEvent.click(settingsButton);

      await waitFor(() => {
        expect(screen.getByText('Settings')).toBeInTheDocument();
      });
    });
  });

  describe('header', () => {
    it('renders app title', () => {
      useContainersHook.useContainers.mockReturnValue({
        data: [],
        isLoading: false,
        error: null,
        refetch: vi.fn(),
      });

      renderWithQuery(<App />);

      expect(screen.getByText('Porthole')).toBeInTheDocument();
    });
  });

  describe('Docker health', () => {
    it('shows Docker unavailable when Docker is down', () => {
      useContainersHook.useContainers.mockReturnValue({
        data: [],
        isLoading: false,
        error: null,
        refetch: vi.fn(),
      });

      useDockerHealthHook.useDockerHealth.mockReturnValue({
        data: { status: 'DOWN' },
        isLoading: false,
        isError: false,
      });

      renderWithQuery(<App />);

      expect(screen.getByText(/Docker unavailable/)).toBeInTheDocument();
    });
  });
});

