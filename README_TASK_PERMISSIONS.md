# FamilyPoints Task Permissions v1

Overlay based on the uploaded current backend source snapshot.

Files:
- TaskAuthorizationService.java (new)
- TaskService.java (updated)
- TaskController.java (updated)

Behavior:
- Central task authorization service.
- CREATE_TASKS required to create definitions.
- Author, Task ADMIN, MANAGE_TASKS, or ADMIN_OVERRIDE can manage definitions.
- OBSERVER can read but cannot execute/manage.
- EXECUTOR can read and execute task instances.
- Current concrete executor can execute.
- VIEW_ALL_TASKS can read all task definitions in the workspace.
- Open task claim is self-only and still requires eligibility.
- Delegation requires delegationAllowed and manage/execute access; target eligibility remains validated.
- miss/excuse are administrative actions.
- All TaskController endpoints require access to the workspace.
- /generate and /process-deadlines require MANAGE_TASKS.

Database/Flyway:
- No schema change. Do NOT create V2 for this overlay.

After applying:
1. Stop backend.
2. Replace/add these files preserving paths.
3. Run Maven compile/test.
4. Start backend.
5. Smoke test as parent/admin and as Denys/child account.
6. If OK, commit as a separate Git commit.

Note:
This overlay was inspected against the uploaded source snapshot, but cannot be full-project Maven compiled here because the uploaded archive contains the Java package snapshot rather than the complete Maven project.
