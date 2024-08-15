package org.avarion.yaml;

import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

class NestedMap {
	private final Map<String, Object> map = new LinkedHashMap<>();

	@SuppressWarnings("unchecked")
	public void put(@Nullable String key, @Nullable String comment, Object value) {
		String[] keys = key.split("\\.");

		Map<String, Object> current = map;
		for (int i = 0; i < keys.length - 1; i++) {
			final String k = keys[i];
			current = (Map<String, Object>) current.computeIfAbsent(k, x -> new LinkedHashMap<>());
		}
		final String lastKey = keys[keys.length - 1];
		if (current.containsKey(lastKey)) {
			throw new RuntimeException("Configuration error: " + lastKey + " is already used before.");
		}

		current.put(lastKey, new NestedNode(value, comment));
	}

	public Map<String, Object> getMap() {
		return map;
	}

	public static class NestedNode {
		public final Object value;
		public final @Nullable String comment;

		public NestedNode(@Nullable Object value, @Nullable String comment) {
			this.value = value;
			this.comment = comment;
		}
	}
}
