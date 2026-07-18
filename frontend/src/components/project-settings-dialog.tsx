"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { Trash, UserPlus, X, Loader2, Save, Users, Settings as SettingsIcon } from "lucide-react";

import { ApiClient, type MemberResponse } from "@/lib/api-client";
import { useWorkspaceStore } from "@/stores/workspace-store";
import { useAuthStore } from "@/stores/auth-store";

import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";

export function ProjectSettingsDialog({
  projectId,
  open,
  onOpenChange,
}: {
  projectId: number;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  const router = useRouter();
  const activeProject = useWorkspaceStore((state) => state.activeProject);
  const setActiveProject = useWorkspaceStore((state) => state.setActiveProject);
  const currentUser = useAuthStore((state) => state.user);

  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [isSaving, setIsSaving] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);

  // Members tab state
  const [members, setMembers] = useState<MemberResponse[]>([]);
  const [isLoadingMembers, setIsLoadingMembers] = useState(false);
  const [inviteEmail, setInviteEmail] = useState("");
  const [inviteRole, setInviteRole] = useState<"EDITOR" | "VIEWER">("EDITOR");
  const [isInviting, setIsInviting] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  useEffect(() => {
    if (open && activeProject) {
      setName(activeProject.name);
      setDescription(activeProject.description || "");
      void loadMembers();
    }
  }, [open, activeProject]);

  async function loadMembers() {
    setIsLoadingMembers(true);
    try {
      const response = await ApiClient.getProjectMembers(projectId);
      setMembers(response.data);
    } catch (e: any) {
      console.error("Failed to load members", e);
    } finally {
      setIsLoadingMembers(false);
    }
  }

  async function onSaveGeneral() {
    setIsSaving(true);
    setErrorMsg(null);
    try {
      const response = await ApiClient.updateProject(projectId, { name, description });
      setActiveProject(response.data);
    } catch (e: any) {
      setErrorMsg(e.response?.data?.message || "Failed to update project");
    } finally {
      setIsSaving(false);
    }
  }

  async function onDeleteProject() {
    if (!confirm("Are you sure you want to delete this project? This action cannot be undone.")) return;
    setIsDeleting(true);
    try {
      await ApiClient.deleteProject(projectId);
      onOpenChange(false);
      router.push("/dashboard");
    } catch (e: any) {
      setErrorMsg(e.response?.data?.message || "Failed to delete project");
      setIsDeleting(false);
    }
  }

  async function onInviteMember(e: React.FormEvent) {
    e.preventDefault();
    if (!inviteEmail) return;
    setIsInviting(true);
    setErrorMsg(null);
    try {
      await ApiClient.inviteMember(projectId, { username: inviteEmail, role: inviteRole });
      setInviteEmail("");
      await loadMembers();
    } catch (e: any) {
      setErrorMsg(e.response?.data?.message || "Failed to invite member");
    } finally {
      setIsInviting(false);
    }
  }

  async function onUpdateRole(memberId: number, newRole: "EDITOR" | "VIEWER" | "OWNER") {
    try {
      await ApiClient.updateMemberRole(projectId, memberId, { role: newRole });
      await loadMembers();
    } catch (e: any) {
      setErrorMsg(e.response?.data?.message || "Failed to update member role");
    }
  }

  async function onRemoveMember(memberId: number) {
    if (!confirm("Are you sure you want to remove this member?")) return;
    try {
      await ApiClient.removeMember(projectId, memberId);
      await loadMembers();
    } catch (e: any) {
      setErrorMsg(e.response?.data?.message || "Failed to remove member");
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="border-white/10 bg-zinc-950/95 backdrop-blur-2xl text-zinc-50 sm:max-w-[500px] rounded-2xl shadow-2xl">
        <DialogHeader className="space-y-1.5">
          <DialogTitle className="text-lg font-bold tracking-tight">Project Settings</DialogTitle>
          <DialogDescription className="text-xs text-zinc-400">
            Manage your workspace settings and team invitations.
          </DialogDescription>
        </DialogHeader>

        {errorMsg && (
          <div className="rounded-xl bg-red-500/10 p-3.5 text-xs text-red-400 border border-red-500/20">
            {errorMsg}
          </div>
        )}

        <Tabs defaultValue="general" className="mt-2">
          <TabsList className="w-full bg-zinc-900/60 border border-white/5 rounded-xl p-1 gap-1">
            <TabsTrigger value="general" className="flex-1 rounded-lg text-xs font-semibold data-[state=active]:bg-zinc-800 data-[state=active]:text-zinc-50 flex items-center justify-center gap-1.5 h-8">
              <SettingsIcon className="size-3.5" />
              General
            </TabsTrigger>
            <TabsTrigger value="members" className="flex-1 rounded-lg text-xs font-semibold data-[state=active]:bg-zinc-800 data-[state=active]:text-zinc-50 flex items-center justify-center gap-1.5 h-8">
              <Users className="size-3.5" />
              Members
            </TabsTrigger>
          </TabsList>

          <TabsContent value="general" className="mt-4 space-y-4">
            <div className="space-y-1.5">
              <Label htmlFor="name" className="text-xs text-zinc-300 font-medium">Project Name</Label>
              <Input
                id="name"
                value={name}
                onChange={(e) => setName(e.target.value)}
                className="h-10 border-white/10 bg-zinc-900/60 focus:border-indigo-500/50 focus:ring-indigo-500/10 focus:ring-2 rounded-xl text-sm"
              />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="desc" className="text-xs text-zinc-300 font-medium">Description</Label>
              <Textarea
                id="desc"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                className="border-white/10 bg-zinc-900/60 focus:border-indigo-500/50 focus:ring-indigo-500/10 focus:ring-2 rounded-xl text-sm min-h-[100px] resize-none"
              />
            </div>

            <div className="flex justify-between items-center pt-4 border-t border-white/5">
              <Button
                variant="destructive"
                onClick={onDeleteProject}
                disabled={isDeleting}
                className="bg-red-500/10 text-red-400 hover:bg-red-500 hover:text-white border border-red-500/20 rounded-xl h-9.5 text-xs font-semibold"
              >
                {isDeleting ? <Loader2 className="mr-2 size-3.5 animate-spin" /> : <Trash className="mr-1.5 size-3.5" />}
                Delete Project
              </Button>
              <Button onClick={onSaveGeneral} disabled={isSaving} className="bg-gradient-to-r from-indigo-50 to-indigo-100 hover:from-white hover:to-white text-zinc-950 font-bold rounded-xl h-9.5 px-4 shadow-lg active:scale-95 transition-all text-xs">
                {isSaving ? <Loader2 className="mr-2 size-3.5 animate-spin" /> : <Save className="mr-1.5 size-3.5" />}
                Save Changes
              </Button>
            </div>
          </TabsContent>

          <TabsContent value="members" className="mt-4 space-y-5">
            <form onSubmit={onInviteMember} className="flex items-end gap-2">
              <div className="flex-1 space-y-1.5">
                <Label className="text-xs text-zinc-300 font-medium">Invite via Email address</Label>
                <div className="flex gap-2">
                  <Input
                    placeholder="teammate@example.com"
                    type="email"
                    required
                    value={inviteEmail}
                    onChange={(e) => setInviteEmail(e.target.value)}
                    className="border-white/10 bg-zinc-900/60 focus:border-indigo-500/50 focus:ring-indigo-500/10 focus:ring-2 rounded-xl text-sm h-9.5 flex-1"
                  />
                  <Select
                    value={inviteRole}
                    onValueChange={(val: any) => setInviteRole(val)}
                  >
                    <SelectTrigger className="w-[110px] h-9.5 border-white/10 bg-zinc-900/60 focus:border-indigo-500/50 focus:ring-indigo-500/10 focus:ring-2 rounded-xl text-xs">
                      <SelectValue placeholder="Role" />
                    </SelectTrigger>
                    <SelectContent className="border-white/10 bg-zinc-950 text-zinc-100 rounded-xl">
                      <SelectItem value="EDITOR">Editor</SelectItem>
                      <SelectItem value="VIEWER">Viewer</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
              </div>
              <Button type="submit" disabled={isInviting} className="h-9.5 bg-indigo-500 hover:bg-indigo-400 text-white font-bold rounded-xl px-4 text-xs shrink-0 transition-all duration-200">
                {isInviting ? <Loader2 className="size-3.5 animate-spin" /> : <UserPlus className="size-3.5 mr-1" />}
                Invite
              </Button>
            </form>

            <div className="space-y-3">
              <p className="text-xs font-bold text-zinc-300 uppercase tracking-wider">Team Members</p>
              {isLoadingMembers ? (
                <div className="flex justify-center py-6"><Loader2 className="size-5 animate-spin text-indigo-400" /></div>
              ) : (
                <div className="space-y-2.5">
                  {members.map((member) => {
                    const initials = member.name
                      ? member.name.split(" ").map((n) => n[0]).join("").slice(0, 2).toUpperCase()
                      : member.username.slice(0, 2).toUpperCase();
                    return (
                      <div key={member.userId} className="flex items-center justify-between p-3 rounded-xl border border-white/5 bg-zinc-900/20 hover:bg-zinc-900/40 transition-colors duration-200">
                        <div className="flex items-center gap-3">
                          <Avatar className="size-8 border border-white/10">
                            <AvatarFallback className="bg-gradient-to-br from-indigo-500 to-sky-500 text-[10px] font-bold text-white">
                              {initials}
                            </AvatarFallback>
                          </Avatar>
                          <div>
                            <p className="text-xs font-semibold text-zinc-150">{member.name}</p>
                            <p className="text-[10px] text-zinc-500">{member.username}</p>
                          </div>
                        </div>
                        <div className="flex items-center gap-2">
                          <Select
                            value={member.projectRole}
                            onValueChange={(val: any) => onUpdateRole(member.userId, val)}
                            disabled={member.projectRole === "OWNER" || member.userId === currentUser?.id}
                          >
                            <SelectTrigger className="w-[100px] h-8 text-[11px] border-white/10 bg-zinc-900/60 focus:border-indigo-500/50 focus:ring-indigo-500/10 rounded-lg">
                              <SelectValue />
                            </SelectTrigger>
                            <SelectContent className="border-white/10 bg-zinc-950 text-zinc-100 rounded-lg">
                              <SelectItem value="OWNER" disabled>Owner</SelectItem>
                              <SelectItem value="EDITOR">Editor</SelectItem>
                              <SelectItem value="VIEWER">Viewer</SelectItem>
                            </SelectContent>
                          </Select>
                          
                          {member.projectRole !== "OWNER" && member.userId !== currentUser?.id && (
                            <Button 
                              variant="ghost" 
                              size="icon" 
                              onClick={() => onRemoveMember(member.userId)}
                              className="text-zinc-400 hover:text-red-400 hover:bg-red-500/10 h-8 w-8 rounded-lg"
                            >
                              <X className="size-4" />
                            </Button>
                          )}
                        </div>
                      </div>
                    );
                  })}
                  {members.length === 0 && <p className="text-xs text-zinc-500 text-center py-4">No team members found.</p>}
                </div>
              )}
            </div>
          </TabsContent>
        </Tabs>
      </DialogContent>
    </Dialog>
  );
}
