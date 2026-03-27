# Open Source Release Checklist

Use this checklist before the first public push to GitHub.

## Before the first push

- Confirm no secrets are present in tracked files:
  - API keys
  - database passwords
  - cookies
  - access tokens
  - personal data exports
- Confirm generated files are not part of the repository:
  - `frontend/node_modules`
  - `frontend/dist`
  - `backend/target`
  - `backend/uploads`
  - local `.env` files
- Confirm README, LICENSE, CONTRIBUTING, SECURITY, and CODE_OF_CONDUCT are present.
- Confirm backend and frontend can still build from a clean checkout.
- Confirm sample configuration files are enough for a new contributor to start locally.

## If sensitive data was ever committed before

- Rotate the secret immediately.
- Remove the secret from the current working tree.
- Rewrite Git history before making the repository public.
- Do not rely on `.gitignore` alone if the file was already committed.

## Recommended GitHub repository settings

- Enable branch protection for `main`
- Enable Dependabot alerts and security updates
- Enable private vulnerability reporting
- Add repository description, topics, and homepage if available
- Add required reviewers if multiple maintainers collaborate

## Suggested first publish commands

If this folder is not yet a Git repository, run:

```bash
git init
git branch -M main
```

Then publish with:

```bash
git add .
git commit -m "chore: prepare open source release"
git remote add origin <your-github-repo-url>
git push -u origin main
```

## If ignored files were added before

Use `git rm --cached` to untrack them without deleting local copies. Example:

```bash
git rm -r --cached frontend/node_modules frontend/dist backend/target backend/uploads
```

Then commit the cleanup before publishing.
