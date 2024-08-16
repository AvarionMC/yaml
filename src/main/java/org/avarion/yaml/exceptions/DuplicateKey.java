package org.avarion.yaml.exceptions;

public class DuplicateKey extends YamlException {
	public DuplicateKey(final String key) {
		super("'" + key + "' is already used before.");
	}
}
