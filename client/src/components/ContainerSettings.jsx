import Modal from './Modal';

function ContainerSettings({ containerName, ports, selectedPort, checkUpdates, onClose, onSelectPort, onToggleCheckUpdates }) {
    const hasPorts = ports && ports.length > 0;

    return (
        <Modal title={containerName} onClose={onClose} className="settings-modal">
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
        </Modal>
    );
}

export default ContainerSettings;
