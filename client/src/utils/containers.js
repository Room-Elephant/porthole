/**
 * Groups containers by their project name.
 * Containers without a project are grouped under null.
 */
export const groupByProject = (containers) => {
  const groups = {};
  containers.forEach(container => {
    const project = container.project || null;
    if (!groups[project]) {
      groups[project] = [];
    }
    groups[project].push(container);
  });
  return groups;
};

/**
 * Generates a target URL for a given port using the current hostname.
 */
export const getTargetUrl = (port) => {
  const hostname = window.location.hostname;
  return `http://${hostname}:${port}`;
};

