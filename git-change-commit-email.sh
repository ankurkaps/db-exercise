#!/bin/bash

# Check if the script is run in a git repository
if [ ! -d .git ]; then
    echo "This script must be run in a Git repository."
    exit 1
fi

# Prompt for old and new email addresses
read -p "Enter the old email address: " OLD_EMAIL
read -p "Enter the new email address: " NEW_EMAIL

# Display the emails for confirmation
echo "You have entered:"
echo "Old Email: $OLD_EMAIL"
echo "New Email: $NEW_EMAIL"

# Ask for explicit confirmation
read -p "Are you sure you want to change the email from '$OLD_EMAIL' to '$NEW_EMAIL' for all commits? (y/n): " CONFIRM

if [[ "$CONFIRM" != "y" ]]; then
    echo "Operation canceled. No changes were made."
    exit 0
fi

# Run git filter-branch to change the email
git filter-branch --env-filter "
if [ \"\$GIT_COMMITTER_EMAIL\" = \"$OLD_EMAIL\" ]; then
    export GIT_COMMITTER_EMAIL=\"$NEW_EMAIL\"
fi
if [ \"\$GIT_AUTHOR_EMAIL\" = \"$OLD_EMAIL\" ]; then
    export GIT_AUTHOR_EMAIL=\"$NEW_EMAIL\"
fi
" -- --all

# Clean up backup references created by filter-branch
rm -rf .git/refs/original/

# Inform the user
echo "Email address changed from $OLD_EMAIL to $NEW_EMAIL for all commits."

# Prompt for force push
read -p "Do you want to force push the changes to the remote repository? (y/n): " PUSH_CONFIRM
if [[ "$PUSH_CONFIRM" == "y" ]]; then
    git push --force --all
    echo "Changes pushed to the remote repository."
else
    echo "Changes not pushed. Remember to push them manually if needed."
fi

