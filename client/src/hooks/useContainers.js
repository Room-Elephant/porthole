import { useQuery } from '@tanstack/react-query';
import { fetchContainers } from '../api';

export const useContainers = ({ showAll, showStopped }) => {
  return useQuery({
    queryKey: ['containers', { showAll, showStopped }],
    queryFn: ({ signal }) => fetchContainers({ showAll, showStopped, signal }),
    staleTime: 30 * 1000, // Consider data fresh for 30 seconds
    refetchInterval: 30 * 1000, // Poll every 30 seconds for real-time updates
  });
};
