import { memo } from 'react';

function SkeletonTile() {
  return (
    <div className="card skeleton-card">
      <div className="card-actions">
        <span className="skeleton-status"></span>
        <span className="skeleton-button"></span>
      </div>
      <div className="skeleton-icon"></div>
      <div className="container-info">
        <h3 className="skeleton-text skeleton-title"></h3>
        <p className="skeleton-text skeleton-subtitle"></p>
      </div>
    </div>
  );
}

export default memo(SkeletonTile);

