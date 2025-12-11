import { useQuery } from '@tanstack/react-query';
import { fetchContainerVersion } from '../api';

export const useContainerVersion = (containerId, { enabled = true } = {}) => {
  return useQuery({
    queryKey: ['containerVersion', containerId],
    queryFn: ({ signal }) => fetchContainerVersion({ containerId, signal }),
    enabled,
    staleTime: 5 * 60 * 1000,
    gcTime: 10 * 60 * 1000,
    refetchOnMount: false,
    refetchOnWindowFocus: false,
    refetchOnReconnect: false,
  });
};
