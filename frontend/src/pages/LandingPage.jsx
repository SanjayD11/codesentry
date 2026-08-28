import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { isTokenExpired, clearAuthStorage } from '../utils/tokenUtils';

export default function LandingPage() {
  const navigate = useNavigate();

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (token && !isTokenExpired(token)) {
      navigate('/dashboard', { replace: true });
    } else {
      clearAuthStorage();
      window.location.replace('/landing.html');
    }
  }, [navigate]);

  return null;
}

