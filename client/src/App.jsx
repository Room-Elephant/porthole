import { useState, useMemo, lazy, Suspense } from 'react';
import { Settings, Package } from 'lucide-react';
import ContainerTile from './components/ContainerTile';
import SkeletonTile from './components/SkeletonTile';

const AppSettings = lazy(() => import('./components/AppSettings'));
import { useContainers } from './hooks/useContainers';
import { useLocalStorage } from './hooks/useLocalStorage';
import { STORAGE_KEYS } from './constants';
import { groupByProject } from './utils/containers';

function App() {
  const [showSettings, setShowSettings] = useState(false);
  const [showAll, setShowAll] = useLocalStorage(STORAGE_KEYS.SHOW_ALL, false);
  const [showStopped, setShowStopped] = useLocalStorage(STORAGE_KEYS.SHOW_STOPPED, false);

  const { data: containers = [], isLoading, error } = useContainers({ showAll, showStopped });

  // Group containers by project (memoized)
  const { groupedContainers, standaloneContainers, projectNames } = useMemo(() => {
    const groups = groupByProject(containers);
    return {
      groupedContainers: groups,
      standaloneContainers: groups[null] || [],
      projectNames: Object.keys(groups)
        .filter(key => key !== 'null' && key !== null)
        .sort(),
    };
  }, [containers]);

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
        <div className="error-message">
          Failed to fetch containers. Ensure Docker and Server are running.
        </div>
      );
    }

    if (containers.length === 0) {
      return (
        <div className="empty-state">
          <Package size={64} strokeWidth={1} />
          <h2>No containers found</h2>
          <p>
            {!showAll && !showStopped
              ? 'No running containers with exposed ports. Try enabling "Show stopped containers" or "Show containers without ports" in settings.'
              : !showStopped
              ? 'No containers with exposed ports found. Try enabling "Show stopped containers" in settings.'
              : !showAll
              ? 'No running containers found. Try enabling "Show containers without ports" in settings.'
              : 'No containers found. Make sure Docker is running and has containers.'}
          </p>
          <button className="settings-btn-primary" onClick={() => setShowSettings(true)}>
            <Settings size={18} />
            Open Settings
          </button>
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
        <Suspense fallback={null}>
          <AppSettings
            showStopped={showStopped}
            showAll={showAll}
            onToggleShowStopped={setShowStopped}
            onToggleShowAll={setShowAll}
            onClose={() => setShowSettings(false)}
          />
        </Suspense>
      )}
    </div>
  );
}

export default App;
