import { useState } from 'react';
import { Settings } from 'lucide-react';
import ContainerTile from './components/ContainerTile';
import AppSettings from './components/AppSettings';
import SkeletonTile from './components/SkeletonTile';
import { useContainers } from './hooks/useContainers';
import { STORAGE_KEYS } from './constants';

function App() {
  const [showSettings, setShowSettings] = useState(false);
  // Load toggle state from localStorage, default to false
  const [showAll, setShowAll] = useState(() => {
    const saved = localStorage.getItem(STORAGE_KEYS.SHOW_ALL);
    return saved === 'true';
  });
  const [showStopped, setShowStopped] = useState(() => {
    const saved = localStorage.getItem(STORAGE_KEYS.SHOW_STOPPED);
    return saved === 'true';
  });

  const { data: containers = [], isLoading, error } = useContainers({ showAll, showStopped });

  const handleToggleChange = (checked) => {
    setShowAll(checked);
    localStorage.setItem(STORAGE_KEYS.SHOW_ALL, checked.toString());
  };

  const handleShowStoppedChange = (checked) => {
    setShowStopped(checked);
    localStorage.setItem(STORAGE_KEYS.SHOW_STOPPED, checked.toString());
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
    if (isLoading) {
      return (
        <div className="container-grid">
          {[...Array(6)].map((_, i) => (
            <SkeletonTile key={i} />
          ))}
        </div>
      );
    }

    if (error) {
      return (
        <div style={{ color: 'red' }}>
          Failed to fetch containers. Ensure Docker and Server are running.
        </div>
      );
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
              <Settings size={24} />
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
