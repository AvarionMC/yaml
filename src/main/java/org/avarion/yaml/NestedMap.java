package org.avarion.yaml;

import org.avarion.yaml.exceptions.DuplicateKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

class NestedMap {
	private final Map<Object, Object> map = new LinkedHashMap<>();

	@SuppressWarnings("unchecked")
	public void put(@NotNull String key, @Nullable String comment, Object value) throws DuplicateKey {
		put(key, comment, value, null);
	}

	@SuppressWarnings("unchecked")
	public void put(@NotNull String key, @Nullable String comment, Object value, @Nullable Class<?> declaredType) throws DuplicateKey {
		String[] keys = key.split("\\.");

		Map<Object, Object> current = map;
		for (int i = 0; i < keys.length - 1; i++) {
			final String k = keys[i];
			current = (Map<Object, Object>) current.computeIfAbsent(k, x -> new LinkedHashMap<>());
		}
		final String lastKey = keys[keys.length - 1];
		if (current.containsKey(lastKey)) {
			throw new DuplicateKey(key);
		}

		current.put(lastKey, new NestedNode(value, comment, declaredType));
	}

	public Map<Object, Object> getMap() {
		return map;
	}

	public static class NestedNode {
		public final Object value;
		public final @Nullable String comment;
		public final @Nullable Class<?> declaredType;

		public NestedNode(@Nullable Object value, @Nullable String comment) {
			this(value, comment, null);
		}

		public NestedNode(@Nullable Object value, @Nullable String comment, @Nullable Class<?> declaredType) {
			this.value = value;
			this.comment = comment;
			this.declaredType = declaredType;
		}
	}
}
