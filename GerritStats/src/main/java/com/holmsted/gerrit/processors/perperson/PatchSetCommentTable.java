package com.holmsted.gerrit.processors.perperson;

import com.holmsted.gerrit.Commit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

public class PatchSetCommentTable implements Map<Commit, List<Commit.PatchSetComment>> {

    private final Map<Commit, List<Commit.PatchSetComment>> commitToComment = new Hashtable<>();
    private final Map<Commit.PatchSetComment, Commit> commentToCommit = new Hashtable<>();
    private final List<Commit.PatchSetComment> allComments = new ArrayList<>();

    public void addCommentForCommit(@Nonnull Commit commit, @Nonnull Commit.PatchSetComment patchSetComment) {
        List<Commit.PatchSetComment> patchSetComments = commitToComment.computeIfAbsent(commit,
                keyCommit -> new ArrayList<>());
        patchSetComments.add(patchSetComment);

        commentToCommit.put(patchSetComment, commit);
        allComments.add(patchSetComment);

        commitToComment.put(commit, patchSetComments);
    }

    @Nonnull
    public List<Commit.PatchSetComment> getAllComments() {
        return allComments;
    }

    @Override
    public int size() {
        return commitToComment.size();
    }

    @Override
    public boolean isEmpty() {
        return commitToComment.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return commitToComment.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return commitToComment.containsValue(value);
    }

    @Override
    public List<Commit.PatchSetComment> get(Object key) {
        return commitToComment.get(key);
    }

    @Override
    public List<Commit.PatchSetComment> put(Commit key, List<Commit.PatchSetComment> value) {
        throw new UnsupportedOperationException("Call addCommentForCommit()");
    }

    @Override
    public List<Commit.PatchSetComment> remove(Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map<? extends Commit, ? extends List<Commit.PatchSetComment>> map) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Commit> keySet() {
        return commitToComment.keySet();
    }

    @Override
    public Collection<List<Commit.PatchSetComment>> values() {
        return commitToComment.values();
    }

    @Override
    public Set<Entry<Commit, List<Commit.PatchSetComment>>> entrySet() {
        return commitToComment.entrySet();
    }
}
