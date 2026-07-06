# Email Verification on Profile Change

This plan outlines the steps to implement email verification when a user changes their email address in their profile.

## User Review Required

> [!IMPORTANT]
> - The user's email will NOT update immediately upon calling `PUT /users/me`. Instead, it will trigger a verification email to the **new** address.
> - A new endpoint `GET /auth/verify-email-change` will be added to finalize the update.
> - For development, the verification "email" will be logged to the console/logs instead of being sent via a real SMTP server.

## Proposed Changes

### Database & Models

#### [NEW] [V15__Add_email_verification.sql](file:///C:/Users/Mark/AndroidStudioProjects/PublicRepo/fintrack-backend/src/main/resources/db/migration/V15__Add_email_verification.sql)
Add `is_email_verified` to `users` table and create `email_verification_tokens` table.

#### [MODIFY] [User.kt](file:///C:/Users/Mark/AndroidStudioProjects/PublicRepo/fintrack-backend/src/main/kotlin/feature/user/domain/User.kt)
Add `isEmailVerified` field.

#### [MODIFY] [UserDto.kt](file:///C:/Users/Mark/AndroidStudioProjects/PublicRepo/fintrack-backend/src/main/kotlin/feature/user/data/model/UserDto.kt)
Add `isEmailVerified` field.

#### [NEW] [EmailVerificationToken.kt](file:///C:/Users/Mark/AndroidStudioProjects/PublicRepo/fintrack-backend/src/main/kotlin/feature/auth/domain/EmailVerificationToken.kt)
Domain model for verification tokens.

---

### Data Layer

#### [MODIFY] [UserTable.kt](file:///C:/Users/Mark/AndroidStudioProjects/PublicRepo/fintrack-backend/src/main/kotlin/feature/user/data/UserTable.kt)
Add `is_email_verified` column to `UsersTable`.

#### [NEW] [EmailVerificationTokensTable.kt](file:///C:/Users/Mark/AndroidStudioProjects/PublicRepo/fintrack-backend/src/main/kotlin/feature/auth/data/EmailVerificationTokensTable.kt)
Exposed table definition for tokens.

#### [NEW] [EmailVerificationRepository.kt](file:///C:/Users/Mark/AndroidStudioProjects/PublicRepo/fintrack-backend/src/main/kotlin/feature/auth/domain/EmailVerificationRepository.kt)
Interface for token management.

#### [NEW] [ExposedEmailVerificationRepository.kt](file:///C:/Users/Mark/AndroidStudioProjects/PublicRepo/fintrack-backend/src/main/kotlin/feature/auth/data/ExposedEmailVerificationRepository.kt)
Exposed implementation of the repository.

#### [MODIFY] [UserRepositoryImpl.kt](file:///C:/Users/Mark/AndroidStudioProjects/PublicRepo/fintrack-backend/src/main/kotlin/feature/user/data/UserRepositoryImpl.kt)
Handle the new `is_email_verified` field and add `updateEmailVerificationStatus` and `updateEmail` methods.

---

### Service Layer

#### [NEW] [EmailService.kt](file:///C:/Users/Mark/AndroidStudioProjects/PublicRepo/fintrack-backend/src/main/kotlin/core/EmailService.kt)
Interface for sending emails.

#### [NEW] [LogEmailService.kt](file:///C:/Users/Mark/AndroidStudioProjects/PublicRepo/fintrack-backend/src/main/kotlin/core/LogEmailService.kt)
Development implementation that logs the verification link.

#### [MODIFY] [UserServiceImpl.kt](file:///C:/Users/Mark/AndroidStudioProjects/PublicRepo/fintrack-backend/src/main/kotlin/feature/user/domain/UserServiceImpl.kt)
Update `updateUser` logic:
- If email is changing, generate token and send email instead of updating DB immediately.

---

### API Layer

#### [MODIFY] [AuthRoutes.kt](file:///C:/Users/Mark/AndroidStudioProjects/PublicRepo/fintrack-backend/src/main/kotlin/feature/auth/AuthRoutes.kt)
Add `GET /auth/verify-email-change` route.

#### [MODIFY] [AuthModule.kt](file:///C:/Users/Mark/AndroidStudioProjects/PublicRepo/fintrack-backend/src/main/kotlin/feature/auth/di/AuthModule.kt)
Register new repositories and services in Koin.

## Verification Plan

### Automated Tests
- Unit tests for `UserServiceImpl` to ensure tokens are created on email change.
- Integration tests for the verification endpoint.

### Manual Verification
1.  Call `PUT /users/me` with a new email.
2.  Check backend logs for the verification link.
3.  Navigate to the verification link (or call it via Curl/Postman).
4.  Verify `GET /users/me` returns the updated email and `isEmailVerified: true`.
