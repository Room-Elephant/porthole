import { useState, useEffect } from 'react';
import axios from 'axios';
import ContainerTile from './components/ContainerTile';

function App() {
  const [containers, setContainers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  // Load toggle state from localStorage, default to false
  const [showAll, setShowAll] = useState(() => {
    const saved = localStorage.getItem('porthole_showAll');
    return saved === 'true';
  });

  useEffect(() => {
    fetchContainers();
  }, [showAll]); // Refetch when showAll changes

  const fetchContainers = async () => {
    try {
      // Use relative path for single JAR deployment
      const response = await axios.get(`/api/containers?includeWithoutPorts=${showAll}`);
      setContainers(response.data);
      setLoading(false);
    } catch (err) {
      console.error(err);
      setError('Failed to fetch containers. Ensure Docker and Server are running.');
      setLoading(false);
    }
  };

  const handleToggleChange = (checked) => {
    setShowAll(checked);
    localStorage.setItem('porthole_showAll', checked.toString());
  };

  // Group containers by project
  const groupByProject = (containers) => {
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

  if (loading) return <div>Loading...</div>;
  if (error) return <div style={{ color: 'red' }}>{error}</div>;

  const groupedContainers = groupByProject(containers);
  const standaloneContainers = groupedContainers[null] || [];
  const projectNames = Object.keys(groupedContainers)
    .filter(key => key !== 'null' && key !== null)
    .sort();

  return (
    <div>
      <div className="app-container">
        <header className="app-header">
          <h1>Porthole</h1>
          <div className="controls">
            <label className="toggle-switch">
              <input
                type="checkbox"
                checked={showAll}
                onChange={(e) => handleToggleChange(e.target.checked)}
              />
              <span className="toggle-slider"></span>
              <span className="toggle-label">Show containers without ports</span>
            </label>
          </div>
        </header>

        {/* Render standalone containers without section header */}
        {standaloneContainers.length > 0 && (
          <div className="container-grid">
            {standaloneContainers.map(container => (
              <ContainerTile key={container.id} container={container} />
            ))}
          </div>
        )}

        {/* Render project sections with headers */}
        {projectNames.map(projectName => (
          <div key={projectName} className="project-section">
            <h2 className="project-title">{projectName}</h2>
            <div className="container-grid">
              {groupedContainers[projectName].map(container => (
                <ContainerTile key={container.id} container={container} />
              ))}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

export default App;
