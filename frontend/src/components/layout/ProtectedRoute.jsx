import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "@/context/AuthContext";
import { UserRole } from "@/types/enums";

// role: UserRole.USER | UserRole.ADMIN | undefined (any authenticated role)
export function ProtectedRoute({ role }) {
  const { isAuthenticated, user } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (role && user.role !== role) {
    // admins can't use the member flows (POST /subscriptions is USER-only on the backend),
    // and members can't reach admin screens — bounce each to their own home
    return <Navigate to={user.role === UserRole.ADMIN ? "/admin" : "/dashboard"} replace />;
  }

  return <Outlet />;
}

// Keeps already-logged-in users off /login and /register
export function GuestOnlyRoute() {
  const { isAuthenticated, user } = useAuth();

  if (isAuthenticated) {
    return <Navigate to={user.role === UserRole.ADMIN ? "/admin" : "/dashboard"} replace />;
  }

  return <Outlet />;
}
