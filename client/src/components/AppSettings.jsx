import Modal from './Modal';

function AppSettings({ 
    showStopped, 
    showAll, 
    onToggleShowStopped, 
    onToggleShowAll, 
    onClose 
}) {
    return (
        <Modal title="Settings" onClose={onClose} className="app-settings-modal">
            {/* Containers Section */}
            <div className="settings-section">
                <h4 className="settings-section-title">Containers</h4>
                
                <label className="toggle-switch settings-toggle">
                    <input
                        type="checkbox"
                        checked={showStopped}
                        onChange={(e) => onToggleShowStopped(e.target.checked)}
                    />
                    <span className="toggle-slider"></span>
                    <span className="toggle-label">Show stopped containers</span>
                </label>
                
                <label className="toggle-switch settings-toggle settings-toggle-spaced">
                    <input
                        type="checkbox"
                        checked={showAll}
                        onChange={(e) => onToggleShowAll(e.target.checked)}
                    />
                    <span className="toggle-slider"></span>
                    <span className="toggle-label">Show containers without ports</span>
                </label>
            </div>
        </Modal>
    );
}

export default AppSettings;

