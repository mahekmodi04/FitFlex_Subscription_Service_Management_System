import { createContext, useContext, useEffect, useMemo, useState, useCallback } from "react";
import { login as loginRequest, register as registerRequest } from "@/api/auth";
import { TOKEN_STORAGE_KEY, registerUnauthorizedHandler } from "@/api/client";
import { UserRole } from "@/types/enums";

const USER_STORAGE_KEY = "fitflex_user";

const AuthContext = createContext(null);

function readStoredUser() {
  try {
    const raw = localStorage.getItem(USER_STORAGE_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(readStoredUser);

  const persist = useCallback((authResponse) => {
    const nextUser = {
      id: authResponse.userId,
      name: authResponse.name,
      email: authResponse.email,
      role: authResponse.role,
    };
    localStorage.setItem(TOKEN_STORAGE_KEY, authResponse.token);
    localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(nextUser));
    setUser(nextUser);
    return nextUser;
  }, []);

  const updateUser = useCallback((partial) => {
    setUser((prev) => {
      const next = { ...prev, ...partial };
      localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(next));
      return next;
    });
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem(TOKEN_STORAGE_KEY);
    localStorage.removeItem(USER_STORAGE_KEY);
    setUser(null);
  }, []);

  useEffect(() => {
    registerUnauthorizedHandler(() => logout());
  }, [logout]);

  const login = useCallback(
    async (email, password) => {
      const response = await loginRequest({ email, password });
      return persist(response);
    },
    [persist]
  );

  const register = useCallback(
    async (name, email, password) => {
      await registerRequest({ name, email, password });
      // registration doesn't return a token — log in right after so the UX is one step
      return login(email, password);
    },
    [login]
  );

  const value = useMemo(
    () => ({
      user,
      isAuthenticated: !!user,
      isAdmin: user?.role === UserRole.ADMIN,
      login,
      register,
      logout,
      updateUser,
    }),
    [user, login, register, logout, updateUser]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within an AuthProvider");
  return ctx;
}
