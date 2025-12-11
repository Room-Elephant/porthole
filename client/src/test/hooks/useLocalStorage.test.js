import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { renderHook, act, waitFor } from '@testing-library/react';
import { useLocalStorage } from '../../hooks/useLocalStorage';

describe('useLocalStorage', () => {
  beforeEach(() => {
    localStorage.getItem.mockReturnValue(null);
    vi.clearAllMocks();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should return default value when localStorage is empty', () => {
    const { result } = renderHook(() => useLocalStorage('testKey', 'defaultValue'));
    expect(result.current[0]).toBe('defaultValue');
  });

  it('should return stored value from localStorage', () => {
    localStorage.getItem.mockReturnValue('"storedValue"');
    
    const { result } = renderHook(() => useLocalStorage('testKey', 'defaultValue'));
    
    expect(result.current[0]).toBe('storedValue');
  });

  it('should store value in localStorage when set', () => {
    const { result } = renderHook(() => useLocalStorage('testKey', 'defaultValue'));
    
    act(() => {
      result.current[1]('newValue');
    });
    
    expect(result.current[0]).toBe('newValue');
    expect(localStorage.setItem).toHaveBeenCalledWith('testKey', '"newValue"');
  });

  it('should handle boolean values', () => {
    localStorage.getItem.mockReturnValue('true');
    
    const { result } = renderHook(() => useLocalStorage('boolKey', false));
    
    expect(result.current[0]).toBe(true);
    
    act(() => {
      result.current[1](false);
    });
    
    expect(result.current[0]).toBe(false);
    expect(localStorage.setItem).toHaveBeenCalledWith('boolKey', 'false');
  });

  it('should handle object values', () => {
    const defaultObj = { foo: 'bar' };
    localStorage.getItem.mockReturnValue('{"stored":"object"}');
    
    const { result } = renderHook(() => useLocalStorage('objKey', defaultObj));
    
    expect(result.current[0]).toEqual({ stored: 'object' });
  });

  it('should handle null stored value', () => {
    localStorage.getItem.mockReturnValue(null);
    
    const { result } = renderHook(() => useLocalStorage('nullKey', 'default'));
    
    expect(result.current[0]).toBe('default');
  });

  it('should handle invalid JSON in localStorage', () => {
    localStorage.getItem.mockReturnValue('not-valid-json');
    
    const { result } = renderHook(() => useLocalStorage('invalidKey', 'default'));
    
    expect(result.current[0]).toBe('not-valid-json');
  });

  it('should sync value across tabs via storage event', async () => {
    localStorage.getItem.mockReturnValue(null);
    const { result } = renderHook(() => useLocalStorage('syncKey', 'initial'));
    
    expect(result.current[0]).toBe('initial');
    
    await act(async () => {
      const event = new StorageEvent('storage', {
        key: 'syncKey',
        newValue: '"updatedFromOtherTab"',
      });
      window.dispatchEvent(event);
    });
    
    await waitFor(() => {
      expect(result.current[0]).toBe('updatedFromOtherTab');
    });
  });

  it('should not update for different key storage events', async () => {
    localStorage.getItem.mockReturnValue(null);
    const { result } = renderHook(() => useLocalStorage('myKey', 'initial'));
    
    expect(result.current[0]).toBe('initial');
    
    await act(async () => {
      const event = new StorageEvent('storage', {
        key: 'differentKey',
        newValue: '"changed"',
      });
      window.dispatchEvent(event);
    });
    
    expect(result.current[0]).toBe('initial');
  });

  it('should handle localStorage quota exceeded gracefully', () => {
    const consoleWarnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {});
    localStorage.setItem.mockImplementation(() => {
      throw new Error('QuotaExceededError');
    });
    
    const { result } = renderHook(() => useLocalStorage('quotaKey', 'default'));
    
    act(() => {
      result.current[1]('newValue');
    });
    
    expect(result.current[0]).toBe('newValue');
    expect(consoleWarnSpy).toHaveBeenCalled();
    
    consoleWarnSpy.mockRestore();
  });
});
