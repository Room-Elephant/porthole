import { useEffect, useRef } from 'react';

function Modal({ title, children, onClose, className = '' }) {
    const modalRef = useRef(null);

    // Handle Escape key to close modal
    useEffect(() => {
        const handleEscape = (event) => {
            if (event.key === 'Escape') {
                onClose();
            }
        };

        document.addEventListener('keydown', handleEscape);
        return () => document.removeEventListener('keydown', handleEscape);
    }, [onClose]);

    // Focus the modal on mount for accessibility
    useEffect(() => {
        modalRef.current?.focus();
    }, []);

    return (
        <div 
            className="modal-overlay" 
            onClick={onClose}
            role="dialog"
            aria-modal="true"
            aria-labelledby="modal-title"
        >
            <div 
                ref={modalRef}
                className={`modal-content ${className}`} 
                onClick={e => e.stopPropagation()}
                tabIndex={-1}
            >
                <h3 id="modal-title" className="modal-title">{title}</h3>
                {children}
            </div>
        </div>
    );
}

export default Modal;

