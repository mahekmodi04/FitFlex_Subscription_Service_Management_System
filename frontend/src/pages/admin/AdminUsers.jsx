import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { getUsers } from "@/api/users";
import { UserRole } from "@/types/enums";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";

export default function AdminUsers() {
  const [search, setSearch] = useState("");

  const { data: users, isLoading, isError } = useQuery({
    queryKey: ["admin", "users"],
    queryFn: getUsers,
  });

  const filtered = (users ?? []).filter(
    (u) =>
      u.name.toLowerCase().includes(search.trim().toLowerCase()) ||
      u.email.toLowerCase().includes(search.trim().toLowerCase())
  );

  return (
    <div className="pb-12">
      <h1 className="font-display text-2xl font-bold text-ink">Users</h1>
      <p className="mt-1 text-muted-foreground">
        Everyone with an account. Roles aren&apos;t editable from here.
      </p>

      <Input
        placeholder="Search by name or email…"
        value={search}
        onChange={(e) => setSearch(e.target.value)}
        className="mt-6 max-w-xs"
      />

      <div className="mt-6 overflow-x-auto rounded-xl border border-border bg-white">
        {isLoading && (
          <div className="space-y-2 p-4">
            {[0, 1, 2].map((i) => (
              <Skeleton key={i} className="h-10 w-full" />
            ))}
          </div>
        )}

        {isError && <p className="p-8 text-center text-danger">Couldn&apos;t load users.</p>}

        {!isLoading && !isError && filtered.length === 0 && (
          <p className="p-8 text-center text-muted-foreground">No users match your search.</p>
        )}

        {!isLoading && !isError && filtered.length > 0 && (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>ID</TableHead>
                <TableHead>Name</TableHead>
                <TableHead>Email</TableHead>
                <TableHead>Role</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {filtered.map((user) => (
                <TableRow key={user.id}>
                  <TableCell className="text-muted-foreground">#{user.id}</TableCell>
                  <TableCell className="font-medium text-ink">{user.name}</TableCell>
                  <TableCell className="text-muted-foreground">{user.email}</TableCell>
                  <TableCell>
                    <Badge
                      className={
                        user.role === UserRole.ADMIN
                          ? "bg-accent-soft text-accent-foreground"
                          : "bg-muted text-muted-foreground"
                      }
                    >
                      {user.role}
                    </Badge>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </div>
    </div>
  );
}
