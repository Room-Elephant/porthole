import React, { useState } from 'react';
import PortSelector from './PortSelector';

function ContainerTile({ container }) {
    const [showConfig, setShowConfig] = useState(false);
    const isDisabled = !container.hasPublicPorts;

    // Key for local storage
    const storageKey = `port_pref_${container.name}`;

    const getTargetUrl = (port) => {
        const hostname = window.location.hostname;
        return `http://${hostname}:${port}`;
    };

    const handleTileClick = () => {
        if (isDisabled) return; // Don't do anything if disabled

        const preferredPort = localStorage.getItem(storageKey);

        if (preferredPort && container.exposedPorts.includes(parseInt(preferredPort))) {
            window.open(getTargetUrl(preferredPort), '_blank');
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
        if (isDisabled) return; // Don't do anything if disabled

        if (container.exposedPorts.length > 1) {
            setShowConfig(true);
        } else {
            alert("Only one port available: " + container.exposedPorts[0]);
        }
    };

    const handlePortSelect = (port) => {
        localStorage.setItem(storageKey, port);
        setShowConfig(false);
        // Optional: redirect immediately after selection?
        // "When a user clicks on the tile, it should redicrect... If configure button... allows user to select"
        // I will NOT redirect on configure selection, just save.
        // But if the modal was opened via Tile Click, maybe I should?
        // For simplicity, I will just save. User can click tile again.
    };

    return (
        <>
            <div className={`card ${isDisabled ? 'disabled' : ''}`} onClick={handleTileClick}>
                {!isDisabled && (
                    <button className="config-btn" onClick={handleConfigClick} title="Configure Port">
                        <svg xmlns="http://www.w3.org/2001/XMLSchema" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                            <path d="M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.38a2 2 0 0 0-.73-2.73l-.15-.1a2 2 0 0 1-1-1.72v-.51a2 2 0 0 1 1-1.74l-.15-.09a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15-.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z"></path>
                            <circle cx="12" cy="12" r="3"></circle>
                        </svg>
                    </button>
                )}
                <img
                    src={container.name.includes("porthole") ? "porthole.png" : container.iconUrl}
                    alt={container.name}
                    className="container-icon"
                    onError={(e) => { e.target.src = 'https://cdn.jsdelivr.net/gh/homarr-labs/dashboard-icons/webp/docker.webp'; }} // Fallback
                />
                <div className="container-info">
                    <h3>{container.name}</h3>
                    <p className="container-image">
                        {container.updateAvailable && (
                            <span className="update-warning" title={`Update available: ${container.latestVersion}`}>
                                ⚠️
                            </span>
                        )}
                        {container.image}
                    </p>
                </div>
            </div>

            {showConfig && (
                <PortSelector
                    ports={container.exposedPorts}
                    selectedPort={parseInt(localStorage.getItem(storageKey))}
                    onClose={() => setShowConfig(false)}
                    onSelect={handlePortSelect}
                />
            )}
        </>
    );
}

export default ContainerTile;
