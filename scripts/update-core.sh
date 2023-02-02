git submodule update --init --recursive
cd jni/deltachat-core-rust
OLD=`git branch --show-current`
if [ $# -eq 0 ]; then
    echo "updates deltachat-core-rust submodule to last commit of a branch."
    echo "usage: ./scripts/update-core.sh BRANCH_NAME"
    echo "current branch: $OLD"
    exit
fi
BRANCH=$1


git fetch
git checkout $BRANCH
TEST=`git branch --show-current`
if [ "$TEST" != "$BRANCH" ]; then
    echo "cannot select branch: $BRANCH"
    exit
fi
git pull
commitmsg=`git log -1 --pretty=%s`
cd ../..


git add jni/deltachat-core-rust
git commit -m "update deltachat-core-rust to '$commitmsg' of branch '$BRANCH'"
echo "old branch: $OLD, new branch: $BRANCH"
echo "changes are committed to local repo."
echo "use 'git push' to use them or 'git reset HEAD~1; git submodule update --recursive' to abort."
