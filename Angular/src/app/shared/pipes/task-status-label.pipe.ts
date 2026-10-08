import { Pipe, PipeTransform } from '@angular/core';

const TASK_STATUS_LABELS: Record<string, string> = {
  PENDING: 'Chưa thực hiện',
  IN_PROGRESS: 'Đang thực hiện',
  WAITING_APPROVAL: 'Đang chờ duyệt',
  DONE: 'Đã hoàn thành',
  CLOSED_LATE: 'Hoàn thành trễ',
  CANCELLED: 'Đã huỷ'
};

/**
 * Single source of truth for Task status labels — was copy-pasted
 * identically into task-list and task-detail; any edit to one would
 * silently drift from the other.
 */
@Pipe({
  name: 'taskStatusLabel',
  standalone: true
})
export class TaskStatusLabelPipe implements PipeTransform {
  transform(status?: string | null): string {
    return TASK_STATUS_LABELS[status || ''] || status || '';
  }
}
