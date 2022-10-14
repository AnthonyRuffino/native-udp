STASH_NAME="pre-commit-$(date +%s)"
git stash save --quiet --keep-index --include-untracked $STASH_NAME

# Test prospective commit
...

STASHES=$(git stash list)
if [[ $STASHES == *"$STASH_NAME" ]]; then
  git stash pop --quiet
fi

./gradlew build