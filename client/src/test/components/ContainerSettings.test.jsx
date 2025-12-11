import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import ContainerSettings from '../../components/ContainerSettings';

describe('ContainerSettings', () => {
  const defaultProps = {
    containerName: 'nginx',
    ports: [80, 443],
    selectedPort: 80,
    checkUpdates: true,
    onClose: vi.fn(),
    onSelectPort: vi.fn(),
    onToggleCheckUpdates: vi.fn(),
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render container name as title', () => {
    render(<ContainerSettings {...defaultProps} />);
    expect(screen.getByText('nginx')).toBeInTheDocument();
  });

  it('should render port options', () => {
    render(<ContainerSettings {...defaultProps} />);

    expect(screen.getByText(':80')).toBeInTheDocument();
    expect(screen.getByText(':443')).toBeInTheDocument();
  });

  it('should show checkmark for selected port', () => {
    render(<ContainerSettings {...defaultProps} selectedPort={80} />);

    const selectedPortOption = screen.getByText(':80').closest('.port-option');
    expect(selectedPortOption).toHaveClass('selected');
    expect(selectedPortOption).toHaveTextContent('✓');
  });

  it('should call onSelectPort when port is clicked', () => {
    const onSelectPort = vi.fn();
    render(<ContainerSettings {...defaultProps} onSelectPort={onSelectPort} />);

    fireEvent.click(screen.getByText(':443'));

    expect(onSelectPort).toHaveBeenCalledWith(443);
  });

  it('should render version updates toggle', () => {
    render(<ContainerSettings {...defaultProps} />);

    expect(screen.getByText('Check for updates')).toBeInTheDocument();
    expect(screen.getByRole('checkbox')).toBeInTheDocument();
  });

  it('should have toggle checked when checkUpdates is true', () => {
    render(<ContainerSettings {...defaultProps} checkUpdates={true} />);

    const checkbox = screen.getByRole('checkbox');
    expect(checkbox).toBeChecked();
  });

  it('should have toggle unchecked when checkUpdates is false', () => {
    render(<ContainerSettings {...defaultProps} checkUpdates={false} />);

    const checkbox = screen.getByRole('checkbox');
    expect(checkbox).not.toBeChecked();
  });

  it('should call onToggleCheckUpdates when toggle is changed', () => {
    const onToggleCheckUpdates = vi.fn();
    render(
      <ContainerSettings
        {...defaultProps}
        checkUpdates={false}
        onToggleCheckUpdates={onToggleCheckUpdates}
      />
    );

    fireEvent.click(screen.getByRole('checkbox'));

    expect(onToggleCheckUpdates).toHaveBeenCalledWith(true);
  });

  it('should call onClose when modal is closed', () => {
    const onClose = vi.fn();
    render(<ContainerSettings {...defaultProps} onClose={onClose} />);

    fireEvent.keyDown(document, { key: 'Escape' });

    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it('should not render port section when no ports', () => {
    render(<ContainerSettings {...defaultProps} ports={[]} />);

    expect(screen.queryByText('Port Selection')).not.toBeInTheDocument();
    expect(screen.getByText('Version Updates')).toBeInTheDocument();
  });

  it('should render description text', () => {
    render(<ContainerSettings {...defaultProps} />);

    expect(
      screen.getByText(/When enabled, Porthole will check if newer versions are available/)
    ).toBeInTheDocument();
  });
});
