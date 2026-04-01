import { useEffect } from 'react';
import { useAuth0 } from '@auth0/auth0-react';
import { setTokenProvider } from '../api.js';

/**
 * Call this hook once near the top of your authenticated app tree.
 * It registers the Auth0 getAccessTokenSilently function so that
 * the api module can attach bearer tokens to every request.
 */
export function useApiTokenProvider() {
  const { getAccessTokenSilently } = useAuth0();

  useEffect(() => {
    setTokenProvider(getAccessTokenSilently);
  }, [getAccessTokenSilently]);
}
