import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useDockerHealth } from '../../hooks/useDockerHealth';
import * as api from '../../api';

vi.mock('../../api', () => ({
  fetchDockerHealth: vi.fn(),
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

describe('useDockerHealth', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should not fetch when disabled (default)', async () => {
    const { result } = renderHook(
      () => useDockerHealth(),
      { wrapper: createWrapper() }
    );

    await new Promise((resolve) => setTimeout(resolve, 50));

    expect(api.fetchDockerHealth).not.toHaveBeenCalled();
    expect(result.current.data).toBeUndefined();
  });

  it('should fetch when enabled', async () => {
    const mockHealth = { status: 'UP' };
    api.fetchDockerHealth.mockResolvedValue(mockHealth);

    const { result } = renderHook(
      () => useDockerHealth({ enabled: true }),
      { wrapper: createWrapper() }
    );

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(api.fetchDockerHealth).toHaveBeenCalled();
    expect(result.current.data).toEqual(mockHealth);
  });

  it('should handle DOWN status', async () => {
    const mockHealth = { status: 'DOWN' };
    api.fetchDockerHealth.mockResolvedValue(mockHealth);

    const { result } = renderHook(
      () => useDockerHealth({ enabled: true }),
      { wrapper: createWrapper() }
    );

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(result.current.data).toEqual({ status: 'DOWN' });
  });

  it('should handle error without retrying', async () => {
    const error = new Error('Health check failed');
    api.fetchDockerHealth.mockRejectedValue(error);

    const { result } = renderHook(
      () => useDockerHealth({ enabled: true }),
      { wrapper: createWrapper() }
    );

    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });

    expect(api.fetchDockerHealth).toHaveBeenCalledTimes(1);
  });
});
