import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useContainers } from '../../hooks/useContainers';
import * as api from '../../api';

vi.mock('../../api', () => ({
  fetchContainers: vi.fn(),
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

describe('useContainers', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should fetch containers with correct parameters', async () => {
    const mockContainers = [
      { id: '1', name: 'container1' },
      { id: '2', name: 'container2' },
    ];
    api.fetchContainers.mockResolvedValue(mockContainers);

    const { result } = renderHook(
      () => useContainers({ showAll: false, showStopped: true }),
      { wrapper: createWrapper() }
    );

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(api.fetchContainers).toHaveBeenCalledWith(
      expect.objectContaining({
        showAll: false,
        showStopped: true,
      })
    );
    expect(result.current.data).toEqual(mockContainers);
  });

  it('should handle loading state', async () => {
    api.fetchContainers.mockImplementation(
      () => new Promise((resolve) => setTimeout(() => resolve([]), 100))
    );

    const { result } = renderHook(
      () => useContainers({ showAll: false, showStopped: false }),
      { wrapper: createWrapper() }
    );

    expect(result.current.isLoading).toBe(true);

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });
  });

  it('should handle error state', async () => {
    const error = new Error('Failed to fetch');
    api.fetchContainers.mockRejectedValue(error);

    const { result } = renderHook(
      () => useContainers({ showAll: false, showStopped: false }),
      { wrapper: createWrapper() }
    );

    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });

    expect(result.current.error).toBeDefined();
  });

  it('should include signal for cancellation', async () => {
    api.fetchContainers.mockResolvedValue([]);

    renderHook(
      () => useContainers({ showAll: true, showStopped: true }),
      { wrapper: createWrapper() }
    );

    await waitFor(() => {
      expect(api.fetchContainers).toHaveBeenCalledWith(
        expect.objectContaining({
          signal: expect.any(AbortSignal),
        })
      );
    });
  });
});
