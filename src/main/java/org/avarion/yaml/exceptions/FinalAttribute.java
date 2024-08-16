package org.avarion.yaml.exceptions;

public class FinalAttribute extends YamlException {
	public FinalAttribute(final String key) {
		super("'" + key + "' is final. Please adjust this!");
	}
}
