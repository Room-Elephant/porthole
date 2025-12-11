import { useState, useEffect } from 'react';
import axios from 'axios';
import ContainerTile from './components/ContainerTile';
import AppSettings from './components/AppSettings';
import SkeletonTile from './components/SkeletonTile';

function App() {
  const [containers, setContainers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showSettings, setShowSettings] = useState(false);
  // Load toggle state from localStorage, default to false
  const [showAll, setShowAll] = useState(() => {
    const saved = localStorage.getItem('porthole_showAll');
    return saved === 'true';
  });
  const [showStopped, setShowStopped] = useState(() => {
    const saved = localStorage.getItem('porthole_showStopped');
    return saved === 'true';
  });

  useEffect(() => {
    let ignore = false;
    
    const fetchContainers = async () => {
      setLoading(true);
      setError(null);
      setContainers([]);
      try {
        // Use relative path for single JAR deployment
        const response = await axios.get(`/api/containers?includeWithoutPorts=${showAll}&includeStopped=${showStopped}`);
        // TODO: Remove this delay - for testing skeleton loading only
        await new Promise(resolve => setTimeout(resolve, 5000));
        if (!ignore) {
          setContainers(response.data);
          setLoading(false);
        }
      } catch (err) {
        if (!ignore) {
          console.error(err);
          setError('Failed to fetch containers. Ensure Docker and Server are running.');
          setLoading(false);
        }
      }
    };

    fetchContainers();
    
    return () => {
      ignore = true;
    };
  }, [showAll, showStopped]); // Refetch when toggles change

  const handleToggleChange = (checked) => {
    setLoading(true);
    setContainers([]);
    setShowAll(checked);
    localStorage.setItem('porthole_showAll', checked.toString());
  };

  const handleShowStoppedChange = (checked) => {
    setLoading(true);
    setContainers([]);
    setShowStopped(checked);
    localStorage.setItem('porthole_showStopped', checked.toString());
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

  const groupedContainers = groupByProject(containers);
  const standaloneContainers = groupedContainers[null] || [];
  const projectNames = Object.keys(groupedContainers)
    .filter(key => key !== 'null' && key !== null)
    .sort();

  const renderContent = () => {
    if (loading) {
      return (
        <div className="container-grid">
          {[...Array(6)].map((_, i) => (
            <SkeletonTile key={i} />
          ))}
        </div>
      );
    }

    if (error) {
      return <div style={{ color: 'red' }}>{error}</div>;
    }

    return (
      <>
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
      </>
    );
  };

  return (
    <div>
      <div className="app-container">
        <header className="app-header">
          <div className="header-title">
            <h1>Porthole</h1>
            <button className="settings-btn" onClick={() => setShowSettings(true)} title="Settings">
              <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.38a2 2 0 0 0-.73-2.73l-.15-.1a2 2 0 0 1-1-1.72v-.51a2 2 0 0 1 1-1.74l-.15-.09a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15-.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z"></path>
                <circle cx="12" cy="12" r="3"></circle>
              </svg>
            </button>
          </div>
        </header>

        {renderContent()}
      </div>

      {showSettings && (
        <AppSettings
          showStopped={showStopped}
          showAll={showAll}
          onToggleShowStopped={handleShowStoppedChange}
          onToggleShowAll={handleToggleChange}
          onClose={() => setShowSettings(false)}
        />
      )}
    </div>
  );
}

export default App;
