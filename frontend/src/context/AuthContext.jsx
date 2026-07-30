import { createContext, useContext, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import * as authApi from "../api/auth";
import { getAccessToken, clearTokens, onSessionExpired } from "../api/client";
import { getProfile } from "../api/resources";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    const hydrate = async () => {
      if (!getAccessToken()) {
        setLoading(false);
        return;
      }
      try {
        const profile = await getProfile();
        setUser(profile);
      } catch {
        clearTokens();
      } finally {
        setLoading(false);
      }
    };
    hydrate();
  }, []);


  useEffect(() => {
    return onSessionExpired(() => {
      setUser(null);
      setLoading(false);
      navigate("/login", { replace: true });
    });

  }, []);

  const login = async (email, password) => {
    const data = await authApi.login({ email, password });
    setUser(data.user);
    return data;
  };

  const register = async (payload) => {
    const data = await authApi.register(payload);
    setUser(data.user);
    return data;
  };

  const logout = async () => {
    try {
      await authApi.logout(sessionStorage.getItem("vm_rt"));
    } catch {
    } finally {
      setUser(null);
      localStorage.removeItem("vm_org");
    }
  };

  const refreshUser = async () => {
    const profile = await getProfile();
    setUser(profile);
    return profile;
  };

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout, refreshUser }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used inside AuthProvider");
  return ctx;
}
