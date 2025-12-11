import { describe, it, expect } from 'vitest';
import { render } from '@testing-library/react';
import SkeletonTile from '../../components/SkeletonTile';

describe('SkeletonTile', () => {
  it('should render skeleton card', () => {
    render(<SkeletonTile />);

    const card = document.querySelector('.skeleton-card');
    expect(card).toBeInTheDocument();
  });

  it('should render skeleton elements', () => {
    render(<SkeletonTile />);

    expect(document.querySelector('.skeleton-status')).toBeInTheDocument();
    expect(document.querySelector('.skeleton-button')).toBeInTheDocument();
    expect(document.querySelector('.skeleton-icon')).toBeInTheDocument();
    expect(document.querySelector('.skeleton-title')).toBeInTheDocument();
    expect(document.querySelector('.skeleton-subtitle')).toBeInTheDocument();
  });

  it('should have card structure matching ContainerTile', () => {
    render(<SkeletonTile />);

    expect(document.querySelector('.card')).toBeInTheDocument();
    expect(document.querySelector('.card-actions')).toBeInTheDocument();
    expect(document.querySelector('.container-info')).toBeInTheDocument();
  });
});

