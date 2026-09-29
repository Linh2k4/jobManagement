import { Pipe, PipeTransform } from '@angular/core';

const ROLE_LABELS: Record<string, string> = {
  MANAGER: 'Quản lý',
  LEAD: 'Trưởng nhóm',
  MEMBER: 'Nhân viên'
};

/**
 * Single source of truth for the role display label — was copy-pasted
 * (identically, for now) into navbar/profile/user-list; any edit to one
 * would silently drift from the other two.
 */
@Pipe({
  name: 'roleLabel',
  standalone: true
})
export class RoleLabelPipe implements PipeTransform {
  transform(role?: string | null): string {
    return ROLE_LABELS[role || ''] || role || '';
  }
}
