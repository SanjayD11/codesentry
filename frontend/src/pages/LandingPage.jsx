import { useEffect } from 'react';

export default function LandingPage() {
  useEffect(() => {
    // Redirect to the vanilla HTML landing page
    window.location.replace('/landing.html');
  }, []);

  return null;
}
