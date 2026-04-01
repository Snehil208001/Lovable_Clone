"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { Trash, UserPlus, X, Loader2, Save } from "lucide-react";

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
      <DialogContent className="border-white/10 bg-zinc-950 text-zinc-50 sm:max-w-[500px]">
        <DialogHeader>
          <DialogTitle>Project Settings</DialogTitle>
          <DialogDescription className="text-zinc-400">
            Manage your project details and team access.
          </DialogDescription>
        </DialogHeader>

        {errorMsg && (
          <div className="rounded-md bg-red-500/10 p-3 text-sm text-red-400 border border-red-500/20">
            {errorMsg}
          </div>
        )}

        <Tabs defaultValue="general" className="mt-4">
          <TabsList className="w-full bg-zinc-900 border border-white/5">
            <TabsTrigger value="general" className="flex-1 data-[state=active]:bg-zinc-800 data-[state=active]:text-zinc-100">
              General
            </TabsTrigger>
            <TabsTrigger value="members" className="flex-1 data-[state=active]:bg-zinc-800 data-[state=active]:text-zinc-100">
              Members
            </TabsTrigger>
          </TabsList>

          <TabsContent value="general" className="mt-4 space-y-4">
            <div className="space-y-2">
              <Label htmlFor="name">Project Name</Label>
              <Input
                id="name"
                value={name}
                onChange={(e) => setName(e.target.value)}
                className="border-white/10 bg-zinc-900/80"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="desc">Description</Label>
              <Textarea
                id="desc"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                className="border-white/10 bg-zinc-900/80 min-h-[100px]"
              />
            </div>

            <div className="flex justify-between pt-4">
              <Button
                variant="destructive"
                onClick={onDeleteProject}
                disabled={isDeleting}
                className="bg-red-500/20 text-red-400 hover:bg-red-500/30 hover:text-red-300 border border-red-500/20"
              >
                {isDeleting ? <Loader2 className="mr-2 size-4 animate-spin" /> : <Trash className="mr-2 size-4" />}
                Delete Project
              </Button>
              <Button onClick={onSaveGeneral} disabled={isSaving} className="bg-indigo-500 hover:bg-indigo-400">
                {isSaving ? <Loader2 className="mr-2 size-4 animate-spin" /> : <Save className="mr-2 size-4" />}
                Save Changes
              </Button>
            </div>
          </TabsContent>

          <TabsContent value="members" className="mt-4 space-y-5">
            <form onSubmit={onInviteMember} className="flex items-end gap-2">
              <div className="flex-1 space-y-1">
                <Label className="text-xs text-zinc-400">Invite via Email address</Label>
                <div className="flex gap-2">
                  <Input
                    placeholder="teammate@example.com"
                    type="email"
                    required
                    value={inviteEmail}
                    onChange={(e) => setInviteEmail(e.target.value)}
                    className="border-white/10 bg-zinc-900/80 h-9"
                  />
                  <Select
                    value={inviteRole}
                    onValueChange={(val: any) => setInviteRole(val)}
                  >
                    <SelectTrigger className="w-[110px] h-9 border-white/10 bg-zinc-900/80">
                      <SelectValue placeholder="Role" />
                    </SelectTrigger>
                    <SelectContent className="border-white/10 bg-zinc-900 text-zinc-100">
                      <SelectItem value="EDITOR">Editor</SelectItem>
                      <SelectItem value="VIEWER">Viewer</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
              </div>
              <Button type="submit" size="sm" disabled={isInviting} className="h-9 bg-indigo-500 hover:bg-indigo-400 shrink-0">
                {isInviting ? <Loader2 className="size-4 animate-spin" /> : "Invite"}
              </Button>
            </form>

            <div className="space-y-3">
              <p className="text-sm font-medium text-zinc-300">Team Members</p>
              {isLoadingMembers ? (
                <div className="flex justify-center py-4"><Loader2 className="size-5 animate-spin text-zinc-500" /></div>
              ) : (
                <div className="space-y-2">
                  {members.map((member) => (
                    <div key={member.userId} className="flex items-center justify-between p-2 rounded-md border border-white/5 bg-zinc-900/30">
                      <div>
                        <p className="text-sm font-medium">{member.name}</p>
                        <p className="text-xs text-zinc-500">{member.username}</p>
                      </div>
                      <div className="flex items-center gap-2">
                        <Select
                          value={member.projectRole}
                          onValueChange={(val: any) => onUpdateRole(member.userId, val)}
                          disabled={member.projectRole === "OWNER" || member.userId === currentUser?.id}
                        >
                          <SelectTrigger className="w-[100px] h-8 text-xs border-white/10 bg-zinc-900/80">
                            <SelectValue />
                          </SelectTrigger>
                          <SelectContent className="border-white/10 bg-zinc-900 text-zinc-100">
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
                            className="text-zinc-400 hover:text-red-400 hover:bg-red-500/10 h-8 w-8"
                          >
                            <X className="size-4" />
                          </Button>
                        )}
                      </div>
                    </div>
                  ))}
                  {members.length === 0 && <p className="text-sm text-zinc-500">No members found.</p>}
                </div>
              )}
            </div>
          </TabsContent>
        </Tabs>
      </DialogContent>
    </Dialog>
  );
}
