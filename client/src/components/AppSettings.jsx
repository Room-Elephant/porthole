import React from 'react';

function AppSettings({ 
    showStopped, 
    showAll, 
    onToggleShowStopped, 
    onToggleShowAll, 
    onClose 
}) {
    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="modal-content app-settings-modal" onClick={e => e.stopPropagation()}>
                <h3 className="modal-title">Settings</h3>
                
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
                    
                    <label className="toggle-switch settings-toggle" style={{ marginTop: '1rem' }}>
                        <input
                            type="checkbox"
                            checked={showAll}
                            onChange={(e) => onToggleShowAll(e.target.checked)}
                        />
                        <span className="toggle-slider"></span>
                        <span className="toggle-label">Show containers without ports</span>
                    </label>
                </div>
            </div>
        </div>
    );
}

export default AppSettings;

