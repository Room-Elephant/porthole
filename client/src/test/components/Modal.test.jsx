import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import Modal from '../../components/Modal';

describe('Modal', () => {
  const defaultProps = {
    title: 'Test Modal',
    onClose: vi.fn(),
    children: <div>Modal content</div>,
  };

  it('should render with title and children', () => {
    render(<Modal {...defaultProps} />);

    expect(screen.getByText('Test Modal')).toBeInTheDocument();
    expect(screen.getByText('Modal content')).toBeInTheDocument();
  });

  it('should call onClose when overlay is clicked', async () => {
    const onClose = vi.fn();
    render(<Modal {...defaultProps} onClose={onClose} />);

    const overlay = document.querySelector('.modal-overlay');
    fireEvent.click(overlay);

    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it('should not call onClose when modal content is clicked', async () => {
    const onClose = vi.fn();
    render(<Modal {...defaultProps} onClose={onClose} />);

    fireEvent.click(screen.getByText('Modal content'));

    expect(onClose).not.toHaveBeenCalled();
  });

  it('should call onClose when Escape key is pressed', async () => {
    const onClose = vi.fn();
    render(<Modal {...defaultProps} onClose={onClose} />);

    fireEvent.keyDown(document, { key: 'Escape' });

    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it('should have correct accessibility attributes', () => {
    render(<Modal {...defaultProps} />);

    const dialog = screen.getByRole('dialog');
    expect(dialog).toHaveAttribute('aria-modal', 'true');
    expect(dialog).toHaveAttribute('aria-labelledby', 'modal-title');
  });

  it('should apply custom className', () => {
    render(<Modal {...defaultProps} className="custom-modal" />);

    const modalContent = screen.getByText('Modal content').parentElement;
    expect(modalContent).toHaveClass('modal-content');
    expect(modalContent).toHaveClass('custom-modal');
  });

  it('should render title with correct id for accessibility', () => {
    render(<Modal {...defaultProps} />);

    const title = screen.getByText('Test Modal');
    expect(title).toHaveAttribute('id', 'modal-title');
  });
});
