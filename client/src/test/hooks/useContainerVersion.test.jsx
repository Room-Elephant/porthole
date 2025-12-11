import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useContainerVersion } from '../../hooks/useContainerVersion';
import * as api from '../../api';

vi.mock('../../api', () => ({
  fetchContainerVersion: vi.fn(),
}));

const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  });
  return ({ children }) => (
    <QueryClientProvider client={queryClient}>
      {children}
    </QueryClientProvider>
  );
};

describe('useContainerVersion', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should fetch container version when enabled', async () => {
    const mockVersion = {
      currentVersion: '1.0.0',
      latestVersion: '1.1.0',
      updateAvailable: true,
    };
    api.fetchContainerVersion.mockResolvedValue(mockVersion);

    const { result } = renderHook(
      () => useContainerVersion('container-123', { enabled: true }),
      { wrapper: createWrapper() }
    );

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(api.fetchContainerVersion).toHaveBeenCalledWith(
      expect.objectContaining({
        containerId: 'container-123',
      })
    );
    expect(result.current.data).toEqual(mockVersion);
  });

  it('should not fetch when disabled', async () => {
    const { result } = renderHook(
      () => useContainerVersion('container-123', { enabled: false }),
      { wrapper: createWrapper() }
    );

    await new Promise((resolve) => setTimeout(resolve, 50));

    expect(api.fetchContainerVersion).not.toHaveBeenCalled();
    expect(result.current.data).toBeUndefined();
  });

  it('should be enabled by default', async () => {
    api.fetchContainerVersion.mockResolvedValue({ updateAvailable: false });

    renderHook(
      () => useContainerVersion('container-123'),
      { wrapper: createWrapper() }
    );

    await waitFor(() => {
      expect(api.fetchContainerVersion).toHaveBeenCalled();
    });
  });

  it('should handle error state', async () => {
    const error = new Error('Version check failed');
    api.fetchContainerVersion.mockRejectedValue(error);

    const { result } = renderHook(
      () => useContainerVersion('container-123'),
      { wrapper: createWrapper() }
    );

    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });
  });
});
