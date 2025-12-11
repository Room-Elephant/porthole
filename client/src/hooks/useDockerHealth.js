import { useQuery } from '@tanstack/react-query';
import { fetchDockerHealth } from '../api';

export function useDockerHealth({ enabled = false, pollInterval = null } = {}) {
  return useQuery({
    queryKey: ['dockerHealth'],
    queryFn: ({ signal }) => fetchDockerHealth({ signal }),
    enabled,
    retry: false,
    refetchInterval: pollInterval,
    refetchOnWindowFocus: false,
  });
}
