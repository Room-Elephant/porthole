import { useState, useEffect } from 'react';
import axios from 'axios';
import ContainerTile from './components/ContainerTile';

function App() {
  const [containers, setContainers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetchContainers();
  }, []);

  const fetchContainers = async () => {
    try {
      // Use relative path for single JAR deployment
      const response = await axios.get('/api/containers');
      setContainers(response.data);
      setLoading(false);
    } catch (err) {
      console.error(err);
      setError('Failed to fetch containers. Ensure Docker and Server are running.');
      setLoading(false);
    }
  };

  // Group containers by project
  const groupByProject = (containers) => {
    const groups = {};
    containers.forEach(container => {
      const project = container.project || 'Standalone';
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
  const projectNames = Object.keys(groupedContainers).sort();

  return (
    <div>
      <div className="app-container">
        <header className="app-header">
          <h1>Porthole</h1>
        </header>
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
