package org.avarion.yaml;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Carries settings from the keys they used to live under to the ones they live under now.
 *
 * <p>Applied to the parsed file before any field reads it, which is what makes it a migration
 * rather than a read-through: the field is loaded from the new key, and the write-back then
 * persists it there. A release or two later the declaration can be deleted and the files in the
 * wild have already moved.
 *
 * <p>Two spellings, one mechanism. {@link YamlKey#previously()} moves one key; {@link YamlRename}
 * on the class moves a whole block, which is the case a field-level alias cannot reach. Blocks
 * move first, so a field can name an old key that only exists once a block has landed.
 *
 * @see YamlRename
 */
final class KeyRenames {

    private KeyRenames() {
    }

    /**
     * Move everything {@code type} declares as moved, in {@code data}, in place.
     *
     * @return every old path a declaration accounted for, mapped to the key that now holds the
     * value — whether it was carried there or was already there. What it is <em>not</em> is a
     * list of keys still needing a human: those are exactly the ones missing from it.
     */
    static @NotNull Map<String, String> applyTo(final @NotNull Map<String, Object> data,
                                                final @NotNull Class<?> type,
                                                final @NotNull Naming naming) {
        Map<String, String> applied = new LinkedHashMap<>();

        for (YamlRename rename : blockMovesOn(type)) {
            move(data, rename.from(), rename.to(), applied);
        }

        for (Class<?> clazz = type; clazz != null; clazz = clazz.getSuperclass()) {
            for (Field field : clazz.getDeclaredFields()) {
                YamlKey annotation = field.getAnnotation(YamlKey.class);
                if (annotation == null || annotation.previously().length == 0) {
                    continue;
                }
                String to = YamlFileInterface.keyOf(field, annotation, naming);
                for (String from : annotation.previously()) {
                    // Newest first, and the first one the file actually has wins: a file holding
                    // two generations of the same key was hand-edited across an upgrade, and the
                    // later spelling is the better guess at what they meant.
                    if (move(data, from, to, applied)) {
                        break;
                    }
                }
            }
        }

        return applied;
    }

    /**
     * The block moves declared on {@code type} and everything it extends, base class first so a
     * subclass's declaration is applied to a file its parent has already reshaped.
     */
    private static @NotNull List<YamlRename> blockMovesOn(final @NotNull Class<?> type) {
        Deque<Class<?>> chain = new ArrayDeque<>();
        for (Class<?> clazz = type; clazz != Object.class; clazz = clazz.getSuperclass()) {
            chain.push(clazz);
        }
        List<YamlRename> moves = new ArrayList<>();
        for (Class<?> clazz : chain) {
            moves.addAll(Arrays.asList(clazz.getDeclaredAnnotationsByType(YamlRename.class)));
        }
        return moves;
    }

    /**
     * Carry {@code from} to {@code to}, if {@code from} is there at all.
     *
     * @return whether the declaration accounted for {@code from} — either it moved, or {@code to}
     * was already set and the old one was dropped in its favour. False means the file simply did
     * not have it, or that the move could not be made.
     */
    private static boolean move(final @NotNull Map<String, Object> data, final @NotNull String rawFrom,
                                final @NotNull String rawTo, final @NotNull Map<String, String> applied) {
        String from = rawFrom.trim();
        String to = rawTo.trim();
        if (from.isEmpty() || to.isEmpty() || from.equals(to)) {
            return false;
        }

        // The block holding `from` is looked up once and kept: it is both where the value is read
        // and where it is taken out of, and asking twice would mean a second answer to null-check
        // that cannot differ from the first.
        Map<String, Object> origin = blockHolding(data, from, false);
        String leaf = leafOf(from);
        if (origin == null || origin.get(leaf) == null) {
            // Absent, or written down empty. Neither is a value to carry anywhere.
            return false;
        }
        Object value = origin.get(leaf);

        if (valueAt(data, to) != YamlFileInterface.UNKNOWN) {
            // Both generations in one file: somebody hand-edited across an upgrade. The key this
            // release documents is the one to believe, and the other one is about to stop
            // existing, so say which value is being used before it does.
            TypeConverter.warn("'" + from + "' is now '" + to + "', and this file sets both."
                               + " Using '" + to + "'; the value under '" + from + "' is ignored"
                               + " and will not be kept.");
            origin.remove(leaf);
            applied.put(from, to);
            return true;
        }

        if (!putAt(data, to, value)) {
            // Something along the way to `to` is a value where a block has to be. Leaving `from`
            // exactly where it is, is the point: untouched, it is still there to be reported as a
            // key that belongs to nothing, which beats discarding it quietly.
            TypeConverter.warn("'" + from + "' should have moved to '" + to + "', but '" + to
                               + "' cannot be created: something on the way to it is a value"
                               + " rather than a section. Fix that and the move will happen.");
            return false;
        }

        origin.remove(leaf);
        TypeConverter.warn("'" + from + "' is now '" + to + "'; your value has been carried across.");
        applied.put(from, to);
        return true;
    }

    /**
     * Take {@code paths} and everything under them out of {@code data}, so the fields that claim
     * them keep what they already hold.
     *
     * @see YamlFileInterface#load(java.io.File, Set)
     */
    static void drop(final @NotNull Map<String, Object> data, final @NotNull Set<String> paths) {
        for (String path : paths) {
            String trimmed = path.trim();
            Map<String, Object> parent = blockHolding(data, trimmed, false);
            if (parent != null) {
                parent.remove(leafOf(trimmed));
            }
        }
    }

    // ==================== Walking a dotted path ====================

    /** What sits at {@code path}, or {@link YamlFileInterface#UNKNOWN} when nothing does. */
    private static @NotNull Object valueAt(final @NotNull Map<String, Object> data, final @NotNull String path) {
        Map<String, Object> parent = blockHolding(data, path, false);
        String leaf = leafOf(path);
        if (parent == null || !parent.containsKey(leaf)) {
            return YamlFileInterface.UNKNOWN;
        }
        Object value = parent.get(leaf);
        return value == null ? YamlFileInterface.UNKNOWN : value;
    }

    /** Put {@code value} at {@code path}, reporting whether there was room for it. */
    private static boolean putAt(final @NotNull Map<String, Object> data, final @NotNull String path,
                                 final @NotNull Object value) {
        Map<String, Object> parent = blockHolding(data, path, true);
        if (parent == null) {
            return false;
        }
        parent.put(leafOf(path), value);
        return true;
    }

    /**
     * The map that holds the last segment of {@code path}, or {@code null} when the way there is
     * blocked.
     *
     * <p>With {@code create}, missing blocks along the way are made — but a segment that already
     * holds a value rather than a block is never overwritten. Clobbering an operator's setting to
     * make room for a rename would be a worse bug than the one renames exist to fix, so the move
     * is refused instead and said out loud.
     */
    @SuppressWarnings("unchecked")
    private static @Nullable Map<String, Object> blockHolding(final @NotNull Map<String, Object> data,
                                                              final @NotNull String path, final boolean create) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = data;
        for (int i = 0; i < parts.length - 1; i++) {
            Object next = current.get(parts[i]);
            if (!(next instanceof Map)) {
                // An absent key and one written down empty are both room to build in; anything
                // else is somebody's setting.
                if (!create || next != null) {
                    return null;
                }
                next = new LinkedHashMap<String, Object>();
                current.put(parts[i], next);
            }
            current = (Map<String, Object>) next;
        }
        return current;
    }

    private static @NotNull String leafOf(final @NotNull String path) {
        int dot = path.lastIndexOf('.');
        return dot < 0 ? path : path.substring(dot + 1);
    }
}
