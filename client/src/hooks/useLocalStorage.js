import { useState, useCallback, useEffect } from 'react';

const parseStoredValue = (stored, defaultValue) => {
  if (stored === null) return defaultValue;
  try {
    return JSON.parse(stored);
  } catch {
    return stored;
  }
};

export const useLocalStorage = (key, defaultValue) => {
  const [value, setValue] = useState(() => {
    const saved = localStorage.getItem(key);
    return parseStoredValue(saved, defaultValue);
  });

  // Sync across tabs via storage event
  useEffect(() => {
    const handleStorageChange = (event) => {
      if (event.key === key) {
        setValue(parseStoredValue(event.newValue, defaultValue));
      }
    };

    window.addEventListener('storage', handleStorageChange);
    return () => window.removeEventListener('storage', handleStorageChange);
  }, [key, defaultValue]);

  const setStoredValue = useCallback((newValue) => {
    setValue(newValue);
    try {
      localStorage.setItem(key, JSON.stringify(newValue));
    } catch (error) {
      console.warn('localStorage quota exceeded or unavailable:', error);
    }
  }, [key]);

  return [value, setStoredValue];
};

