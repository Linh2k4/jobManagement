import { Component, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
    MatIconModule
  ],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent {
  readonly loading = signal(false);
  readonly hidePassword = signal(true);
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);
  private snackBar = inject(MatSnackBar);

  form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]]
  });

  onSubmit() {
    if (this.form.invalid) return;

    this.loading.set(true);
    const { email, password } = this.form.value;

    this.authService.login(email!, password!).subscribe({
      next: (response) => {
        this.snackBar.open('Login successful', 'Close', { duration: 3000 });
        this.router.navigate(['/dashboard']);
      },
      error: (error) => {
        this.loading.set(false);
        console.error('Login failed:', error);
      }
    });
  }

  togglePasswordVisibility() {
    this.hidePassword.update(v => !v);
  }

  fillAccount(email: string) {
    this.form.patchValue({
      email,
      password: 'admin123'
    });
  }

  get emailError(): string {
    const control = this.form.get('email');
    if (control?.hasError('required')) return 'Email is required';
    if (control?.hasError('email')) return 'Invalid email format';
    return '';
  }

  get passwordError(): string {
    const control = this.form.get('password');
    if (control?.hasError('required')) return 'Password is required';
    if (control?.hasError('minlength')) return 'Password must be at least 6 characters';
    return '';
  }
}
