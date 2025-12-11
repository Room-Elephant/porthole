
function ContainerSettings({ containerName, ports, selectedPort, checkUpdates, onClose, onSelectPort, onToggleCheckUpdates }) {
    const hasPorts = ports && ports.length > 0;

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="modal-content settings-modal" onClick={e => e.stopPropagation()}>
                <h3 className="modal-title">{containerName}</h3>
                
                {/* Port Selection Section */}
                {hasPorts && (
                    <div className="settings-section">
                        <h4 className="settings-section-title">Port Selection</h4>
                        <div className="port-list">
                            {ports.map(port => (
                                <div
                                    key={port}
                                    className={`port-option ${selectedPort === port ? 'selected' : ''}`}
                                    onClick={() => onSelectPort(port)}
                                >
                                    :{port}
                                    {selectedPort === port && <span className="checkmark">✓</span>}
                                </div>
                            ))}
                        </div>
                    </div>
                )}

                {/* Version Updates Section */}
                <div className={`settings-section ${hasPorts ? 'with-divider' : ''}`}>
                    <h4 className="settings-section-title">Version Updates</h4>
                    <label className="toggle-switch settings-toggle">
                        <input
                            type="checkbox"
                            checked={checkUpdates}
                            onChange={(e) => onToggleCheckUpdates(e.target.checked)}
                        />
                        <span className="toggle-slider"></span>
                        <span className="toggle-label">Check for updates</span>
                    </label>
                    <p className="settings-description">
                        When enabled, Porthole will check if newer versions are available for this container.
                    </p>
                </div>
            </div>
        </div>
    );
}

export default ContainerSettings;

