import React from 'react';

function PortSelector({ ports, selectedPort, onClose, onSelect }) {
    // If no ports, nothing to select
    if (!ports || ports.length === 0) return null;

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="modal-content" onClick={e => e.stopPropagation()}>
                <h3 className="modal-title">Select Port</h3>
                <div className="port-list">
                    {ports.map(port => (
                        <div
                            key={port}
                            className={`port-option ${selectedPort === port ? 'selected' : ''}`}
                            onClick={() => onSelect(port)}
                        >
                            :{port}
                            {selectedPort === port && <span style={{ marginLeft: 'auto' }}>✓</span>}
                        </div>
                    ))}
                </div>
            </div>
        </div>
    );
}

export default PortSelector;
