export interface ProjectSummary {
  id: number;
  projectName: string;
  createdAt: string;
  updatedAt: string;
}

export interface ProjectResponse {
  id: number;
  name: string;
  createdAt: string;
  updatedAt: string;
  owner: {
    id: number;
    username: string;
    name: string;
  };
}
