import React, { useState, useEffect } from 'react';
import axios from 'axios';
import ContainerSettings from './ContainerSettings';

function ContainerTile({ container }) {
    const [showConfig, setShowConfig] = useState(false);
    const [versionInfo, setVersionInfo] = useState(null);
    const [versionLoading, setVersionLoading] = useState(false);
    const hasPublicPorts = container.hasPublicPorts;

    // Key for local storage
    const portStorageKey = `port_pref_${container.name}`;
    const versionCheckKey = `version_check_${container.name}`;

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

    useEffect(() => {
        const fetchVersionInfo = async () => {
            if (!checkUpdates) {
                setVersionInfo(null);
                setVersionLoading(false);
                return;
            }

            setVersionLoading(true);
            try {
                const response = await axios.get(`/api/containers/${container.id}/version`);
                setVersionInfo(response.data);
            } catch (err) {
                console.error('Failed to fetch version info:', err);
            } finally {
                setVersionLoading(false);
            }
        };

        fetchVersionInfo();
    }, [container.id, checkUpdates]);

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
                        <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                            <path d="M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.38a2 2 0 0 0-.73-2.73l-.15-.1a2 2 0 0 1-1-1.72v-.51a2 2 0 0 1 1-1.74l-.15-.09a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15-.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z"></path>
                            <circle cx="12" cy="12" r="3"></circle>
                        </svg>
                    </button>
                </div>
                <img
                    src={container.name.includes("porthole") ? "porthole.png" : container.iconUrl}
                    alt={container.name}
                    className="container-icon"
                    onError={(e) => { e.target.src = 'https://cdn.jsdelivr.net/gh/homarr-labs/dashboard-icons/webp/docker.webp'; }} // Fallback
                />
                <div className="container-info">
                    <h3>{container.name}</h3>
                    <p className="container-image">
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
