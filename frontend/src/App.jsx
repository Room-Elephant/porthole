import { useState, useEffect } from 'react';
import axios from 'axios';
import ContainerTile from './components/ContainerTile';

function App() {
  const [containers, setContainers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetchContainers();
  }, []);

  const fetchContainers = async () => {
    try {
      // Use relative path for single JAR deployment
      const response = await axios.get('/api/containers');
      setContainers(response.data);
      setLoading(false);
    } catch (err) {
      console.error(err);
      setError('Failed to fetch containers. Ensure Docker and Server are running.');
      setLoading(false);
    }
  };

  if (loading) return <div>Loading...</div>;
  if (error) return <div style={{ color: 'red' }}>{error}</div>;

  return (
    <div>
      <div className="app-container">
        <header className="app-header">
          <h1>Porthole</h1>
        </header>
        <div className="container-grid">
          {containers.map(container => (
            <ContainerTile key={container.id} container={container} />
          ))}
        </div>
      </div>
    </div>
  );
}

export default App;
