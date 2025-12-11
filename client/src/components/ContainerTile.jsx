import { useState } from 'react';
import { Settings } from 'lucide-react';
import ContainerSettings from './ContainerSettings';
import { useContainerVersion } from '../hooks/useContainerVersion';
import { STORAGE_KEYS, ASSETS } from '../constants';

function ContainerTile({ container }) {
    const [showConfig, setShowConfig] = useState(false);
    const hasPublicPorts = container.hasPublicPorts;

    // Key for local storage
    const portStorageKey = STORAGE_KEYS.PORT_PREF(container.name);
    const versionCheckKey = STORAGE_KEYS.VERSION_CHECK(container.name);

    // Load selectedPort from localStorage
    const [selectedPort, setSelectedPort] = useState(() => {
        const saved = localStorage.getItem(portStorageKey);
        return saved ? parseInt(saved) : null;
    });

    // Load checkUpdates preference from localStorage (default: true)
    const [checkUpdates, setCheckUpdates] = useState(() => {
        const saved = localStorage.getItem(versionCheckKey);
        return saved === null ? true : saved === 'true';
    });

    const { data: versionInfo, isLoading: versionLoading } = useContainerVersion(
        container.id,
        { enabled: checkUpdates }
    );

    // Determine status indicator color based on container state
    const getStatusClass = () => {
        const state = container.state?.toLowerCase();
        if (state === 'running') return 'status-running';
        if (state === 'paused' || state === 'restarting') return 'status-warning';
        return 'status-stopped';
    };

    const getTargetUrl = (port) => {
        const hostname = window.location.hostname;
        return `http://${hostname}:${port}`;
    };

    const handleTileClick = () => {
        if (!hasPublicPorts) return; // Don't do anything if no ports

        if (selectedPort && container.exposedPorts.includes(selectedPort)) {
            window.open(getTargetUrl(selectedPort), '_blank');
            return;
        }

        // No preference or invalid preference
        if (container.exposedPorts.length === 1) {
            window.open(getTargetUrl(container.exposedPorts[0]), '_blank');
        } else {
            // Multiple ports and no preference -> show selection
            setShowConfig(true);
        }
    };

    const handleConfigClick = (e) => {
        e.stopPropagation();
        setShowConfig(true);
    };

    const handlePortSelect = (port) => {
        setSelectedPort(port);
        localStorage.setItem(portStorageKey, port);
        // Don't close the modal, let user continue with other settings
    };

    const handleToggleCheckUpdates = (checked) => {
        setCheckUpdates(checked);
        localStorage.setItem(versionCheckKey, checked.toString());
    };

    return (
        <>
            <div className={`card ${!hasPublicPorts ? 'disabled' : ''}`} onClick={handleTileClick}>
                <div className="card-actions">
                    <span 
                        className={`status-indicator ${getStatusClass()}`} 
                        title={container.status || container.state}
                    ></span>
                    <button className="config-btn" onClick={handleConfigClick} title="Container Settings">
                        <Settings size={20} />
                    </button>
                </div>
                <img
                    src={container.name.includes("porthole") ? ASSETS.PORTHOLE_ICON : container.iconUrl}
                    alt={container.name}
                    className="container-icon"
                    onError={(e) => { e.target.src = ASSETS.FALLBACK_ICON; }}
                />
                <div className="container-info">
                    <h3 title={container.name}>{container.displayName}</h3>
                    <p className="container-image" title={container.image}>
                        {checkUpdates && versionLoading && (
                            <span className="version-loading" title="Checking for updates...">
                                ...
                            </span>
                        )}
                        {checkUpdates && !versionLoading && versionInfo?.updateAvailable && (
                            <span className="update-warning" title={`Update available: ${versionInfo.latestVersion}`}>
                                ⚠️
                            </span>
                        )}
                        {container.image}
                    </p>
                </div>
            </div>

            {showConfig && (
                <ContainerSettings
                    containerName={container.name}
                    ports={container.exposedPorts}
                    selectedPort={selectedPort}
                    checkUpdates={checkUpdates}
                    onClose={() => setShowConfig(false)}
                    onSelectPort={handlePortSelect}
                    onToggleCheckUpdates={handleToggleCheckUpdates}
                />
            )}
        </>
    );
}

export default ContainerTile;
