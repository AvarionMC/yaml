package org.avarion.yaml;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Represent;
import org.yaml.snakeyaml.representer.Representer;

public class ToStringRepresenter extends Representer {
    public ToStringRepresenter() {
        super(new DumperOptions());
        this.representers.put(null, new RepresentFallback());
    }

    private static class RepresentFallback implements Represent {
        public Node representData(Object data) {
            return new ScalarNode(Tag.STR, data.toString(), null, null, DumperOptions.ScalarStyle.PLAIN);
        }
    }
}