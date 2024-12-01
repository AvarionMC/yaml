package org.avarion.yaml.v1;

import org.avarion.yaml.YamlWrapper;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

public class YamlWrapperImpl implements YamlWrapper {
    private final Yaml yaml;

    public YamlWrapperImpl() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        yaml = new Yaml(new ToStringRepresenter(), options);
    }

    @Override
    public String dump(Object data) {
        return yaml.dump(data);
    }

    @Override
    public Object load(String content) {
        return yaml.load(content);
    }
}