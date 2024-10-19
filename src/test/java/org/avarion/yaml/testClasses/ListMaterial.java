package org.avarion.yaml.testClasses;

import org.avarion.yaml.YamlFileInterface;
import org.avarion.yaml.YamlKey;

import java.util.Arrays;
import java.util.List;

public class ListMaterial extends YamlFileInterface {
    @YamlKey("materials")
    public List<Material> materials = Arrays.asList(Material.A, Material.B);

    @YamlKey("enum")
    public Material material = Material.C;
}
