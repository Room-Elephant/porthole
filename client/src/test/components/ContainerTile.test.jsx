import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import ContainerTile from '../../components/ContainerTile';
import * as api from '../../api';
import { createMockContainer } from '../testUtils';

vi.mock('../../api', () => ({
  fetchContainerVersion: vi.fn(),
}));

const renderWithQuery = (ui) => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
        gcTime: 0,
      },
    },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      {ui}
    </QueryClientProvider>
  );
};

describe('ContainerTile', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
    api.fetchContainerVersion.mockResolvedValue({ updateAvailable: false });
  });

  it('should render container name and image', () => {
    const container = createMockContainer({
      displayName: 'My Container',
      image: 'nginx:latest',
    });

    renderWithQuery(<ContainerTile container={container} />);

    expect(screen.getByText('My Container')).toBeInTheDocument();
    expect(screen.getByText('nginx:latest')).toBeInTheDocument();
  });

  it('should render container icon', () => {
    const container = createMockContainer({
      name: 'nginx',
      iconUrl: 'https://example.com/nginx-icon.png',
    });

    renderWithQuery(<ContainerTile container={container} />);

    const icon = screen.getByRole('img');
    expect(icon).toHaveAttribute('src', 'https://example.com/nginx-icon.png');
  });

  it('should use porthole icon for porthole container', () => {
    const container = createMockContainer({
      name: 'porthole-container',
      iconUrl: 'https://example.com/other.png',
    });

    renderWithQuery(<ContainerTile container={container} />);

    const icon = screen.getByRole('img');
    expect(icon).toHaveAttribute('src', 'porthole.png');
  });

  describe('Status indicator', () => {
    it('should show running status', () => {
      const container = createMockContainer({ state: 'running' });
      renderWithQuery(<ContainerTile container={container} />);

      const indicator = document.querySelector('.status-indicator');
      expect(indicator).toHaveClass('status-running');
    });

    it('should show stopped status', () => {
      const container = createMockContainer({ state: 'exited' });
      renderWithQuery(<ContainerTile container={container} />);

      const indicator = document.querySelector('.status-indicator');
      expect(indicator).toHaveClass('status-stopped');
    });

    it('should show warning status for paused', () => {
      const container = createMockContainer({ state: 'paused' });
      renderWithQuery(<ContainerTile container={container} />);

      const indicator = document.querySelector('.status-indicator');
      expect(indicator).toHaveClass('status-warning');
    });

    it('should show warning status for restarting', () => {
      const container = createMockContainer({ state: 'restarting' });
      renderWithQuery(<ContainerTile container={container} />);

      const indicator = document.querySelector('.status-indicator');
      expect(indicator).toHaveClass('status-warning');
    });
  });

  describe('Click behavior', () => {
    it('should open URL on click when container has single port', () => {
      const container = createMockContainer({
        exposedPorts: [8080],
        hasPublicPorts: true,
      });

      renderWithQuery(<ContainerTile container={container} />);

      fireEvent.click(screen.getByRole('img').closest('.card'));

      expect(window.open).toHaveBeenCalledWith('http://localhost:8080', '_blank');
    });

    it('should not open URL when container has no ports', () => {
      const container = createMockContainer({
        exposedPorts: [],
        hasPublicPorts: false,
      });

      renderWithQuery(<ContainerTile container={container} />);

      fireEvent.click(screen.getByRole('img').closest('.card'));

      expect(window.open).not.toHaveBeenCalled();
    });

    it('should show settings modal when container has multiple ports and no preference', () => {
      const container = createMockContainer({
        exposedPorts: [80, 443],
        hasPublicPorts: true,
      });

      renderWithQuery(<ContainerTile container={container} />);

      fireEvent.click(screen.getByRole('img').closest('.card'));

      expect(screen.getByText('Port Selection')).toBeInTheDocument();
    });
  });

  describe('Settings button', () => {
    it('should open settings modal when clicked', () => {
      const container = createMockContainer();

      renderWithQuery(<ContainerTile container={container} />);

      fireEvent.click(screen.getByTitle('Container Settings'));

      expect(screen.getByText(container.name)).toBeInTheDocument();
      expect(screen.getByText('Version Updates')).toBeInTheDocument();
    });

    it('should not trigger card click when settings button is clicked', () => {
      const container = createMockContainer({
        exposedPorts: [8080],
        hasPublicPorts: true,
      });

      renderWithQuery(<ContainerTile container={container} />);

      fireEvent.click(screen.getByTitle('Container Settings'));

      expect(window.open).not.toHaveBeenCalled();
      expect(screen.getByText('Version Updates')).toBeInTheDocument();
    });
  });

  describe('Update indicator', () => {
    it('should show update warning when update available', async () => {
      api.fetchContainerVersion.mockResolvedValue({
        updateAvailable: true,
        latestVersion: '2.0.0',
      });

      const container = createMockContainer();
      renderWithQuery(<ContainerTile container={container} />);

      await waitFor(() => {
        expect(screen.getByText('⚠️')).toBeInTheDocument();
      });
    });

    it('should not show update warning when no update available', async () => {
      api.fetchContainerVersion.mockResolvedValue({
        updateAvailable: false,
      });

      const container = createMockContainer();
      renderWithQuery(<ContainerTile container={container} />);

      await waitFor(() => {
        expect(api.fetchContainerVersion).toHaveBeenCalled();
      });

      expect(screen.queryByText('⚠️')).not.toBeInTheDocument();
    });
  });

  describe('Disabled state', () => {
    it('should have disabled class when no public ports', () => {
      const container = createMockContainer({
        hasPublicPorts: false,
      });

      renderWithQuery(<ContainerTile container={container} />);

      const card = screen.getByRole('img').closest('.card');
      expect(card).toHaveClass('disabled');
    });

    it('should not have disabled class when has public ports', () => {
      const container = createMockContainer({
        hasPublicPorts: true,
      });

      renderWithQuery(<ContainerTile container={container} />);

      const card = screen.getByRole('img').closest('.card');
      expect(card).not.toHaveClass('disabled');
    });
  });
});
