import { TimeCategory } from './task.model';

/** Mirrors the real backend TimelineTaskResponse (GET /tasks/timeline). */
export interface TimelineTask {
  id: number;
  title: string;
  timeCategory: TimeCategory;
  status: string;
  startDate: string;
  endDate: string;
  isOverdue: boolean;
  isDone: boolean;
}

export interface TimelineMember {
  userId: number;
  fullName: string;
  role: string;
  tasks: TimelineTask[];
}
