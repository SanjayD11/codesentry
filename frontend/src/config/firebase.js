import { initializeApp } from "firebase/app";
import { getAuth, GoogleAuthProvider, GithubAuthProvider } from "firebase/auth";

const firebaseConfig = {
  apiKey: "AIzaSyAQF96vGuPxWHLF4--KTOGybHmFH7duzdk",
  authDomain: "ai-security-analysis-platform.firebaseapp.com",
  projectId: "ai-security-analysis-platform",
  storageBucket: "ai-security-analysis-platform.firebasestorage.app",
  messagingSenderId: "466944739419",
  appId: "1:466944739419:web:eabf27cd641aeac493577e"
};

const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);
export const googleProvider = new GoogleAuthProvider();
export const githubProvider = new GithubAuthProvider();

/**
 * ActionCodeSettings for Firebase password reset emails.
 *
 * - url: The page in OUR app that handles the oobCode from the email link.
 *   Must be listed as an Authorized Domain in Firebase Console → Authentication → Settings.
 * - handleCodeInApp: true means Firebase's email link opens our app directly.
 *
 * IMPORTANT: Add your production URL to Firebase Console → Authentication → Settings → Authorized Domains
 *   Development: localhost (already authorized by default in Firebase)
 *   Production:  your-domain.com
 */
export const getPasswordResetActionCodeSettings = () => ({
  url: `${window.location.origin}/reset-password`,
  handleCodeInApp: true,
});
