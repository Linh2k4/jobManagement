/** Scope.md §13 — Group member belongs to, can have multiple. */
export interface Group {
  id: number;
  name: string;
  leadId: number;
  description?: string;
  createdAt: string;
  updatedAt: string;
}

/** One Group membership for a Member (N-N, isPrimary flag). */
export interface GroupMembership {
  id: number;
  groupId: number;
  groupName?: string;
  memberId: number;
  memberName?: string;
  isPrimary: boolean;
  joinedAt: string;
}

export interface GroupResponse {
  id: number;
  name: string;
  description?: string;
  lead: {
    id: number;
    email: string;
    fullName: string;
    role: string;
  };
  memberCount: number;
}

export interface GroupMembershipResponse {
  groupId: number;
  groupName: string;
  member: {
    id: number;
    email: string;
    fullName: string;
    role: string;
  };
  isPrimary: boolean;
  joinedAt: string;
}

export interface CreateGroupRequest {
  name: string;
  description?: string;
  leadId: number;
}

export interface AddGroupMemberRequest {
  memberId: number;
  isPrimary?: boolean;
}
