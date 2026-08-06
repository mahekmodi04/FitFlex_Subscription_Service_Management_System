import { Link, useNavigate } from "react-router-dom";
import { Dumbbell, LogOut, User } from "lucide-react";
import { useAuth } from "@/context/AuthContext";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";

export function Navbar() {
  const { user, isAuthenticated, isAdmin, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate("/");
  };

  const initials = user?.name
    ?.split(" ")
    .map((part) => part[0])
    .slice(0, 2)
    .join("")
    .toUpperCase();

  return (
    <header className="sticky top-0 z-40 border-b border-white/10 bg-ink text-white">
      <div className="mx-auto flex h-16 max-w-6xl items-center justify-between px-4 sm:px-6">
        <Link
          to={isAuthenticated ? (isAdmin ? "/admin" : "/dashboard") : "/"}
          className="flex items-center gap-2 font-display text-lg font-semibold"
        >
          <Dumbbell className="size-5 text-accent" />
          FitFlex
        </Link>

        <nav className="hidden items-center gap-6 text-sm font-medium text-white/80 sm:flex">
          <Link to="/plans" className="hover:text-white">
            Plans
          </Link>
          {isAuthenticated && !isAdmin && (
            <Link to="/billing" className="hover:text-white">
              Billing
            </Link>
          )}
        </nav>

        {isAuthenticated ? (
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <button className="flex items-center gap-2 rounded-full outline-none focus-visible:ring-2 focus-visible:ring-accent">
                <Avatar className="size-8">
                  <AvatarFallback className="bg-accent text-xs font-semibold text-white">
                    {initials}
                  </AvatarFallback>
                </Avatar>
              </button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuLabel className="font-normal">
                <p className="text-sm font-medium">{user.name}</p>
                <p className="text-xs text-muted-foreground">{user.email}</p>
              </DropdownMenuLabel>
              <DropdownMenuSeparator />
              {isAdmin ? (
                <DropdownMenuItem onClick={() => navigate("/admin")}>Admin overview</DropdownMenuItem>
              ) : (
                <>
                  <DropdownMenuItem onClick={() => navigate("/dashboard")}>Dashboard</DropdownMenuItem>
                  <DropdownMenuItem onClick={() => navigate("/account")}>
                    <User className="mr-2 size-4" />
                    Account
                  </DropdownMenuItem>
                </>
              )}
              <DropdownMenuSeparator />
              <DropdownMenuItem onClick={handleLogout} className="text-danger">
                <LogOut className="mr-2 size-4" />
                Log out
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        ) : (
          <div className="flex items-center gap-2">
            <Button asChild variant="ghost" className="text-white hover:bg-white/10 hover:text-white">
              <Link to="/login">Log in</Link>
            </Button>
            <Button asChild>
              <Link to="/register">Get started</Link>
            </Button>
          </div>
        )}
      </div>
    </header>
  );
}
