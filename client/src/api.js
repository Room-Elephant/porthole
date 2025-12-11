import axios from 'axios';
import { API } from './constants';

export const fetchContainers = async ({ showAll, showStopped, signal }) => {
  const response = await axios.get(
    `${API.CONTAINERS}?includeWithoutPorts=${showAll}&includeStopped=${showStopped}`,
    { signal }
  );
  return response.data;
};

export const fetchContainerVersion = async ({ containerId, signal }) => {
  const response = await axios.get(API.CONTAINER_VERSION(containerId), { signal });
  return response.data;
};
