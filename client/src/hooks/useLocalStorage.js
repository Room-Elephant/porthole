import { useState, useCallback, useEffect } from 'react';

const parseStoredValue = (stored, defaultValue) => {
  if (stored === null) return defaultValue;
  try {
    return JSON.parse(stored);
  } catch {
    return stored;
  }
};

/**
 * Custom hook for persisting state in localStorage with cross-tab synchronization.
 * @param {string} key - The localStorage key
 * @param {*} defaultValue - Default value when no stored value exists
 * @returns {[*, function]} Tuple of [value, setValue]
 */
export const useLocalStorage = (key, defaultValue) => {
  const [value, setValue] = useState(() => {
    const saved = localStorage.getItem(key);
    return parseStoredValue(saved, defaultValue);
  });

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
