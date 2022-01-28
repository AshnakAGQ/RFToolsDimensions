package mcjty.rftoolsdim.dimension.descriptor;

import com.google.gson.*;
import mcjty.rftoolsdim.RFToolsDim;
import mcjty.rftoolsdim.modules.dimlets.data.DimletDictionary;
import mcjty.rftoolsdim.modules.dimlets.data.DimletKey;
import mcjty.rftoolsdim.modules.dimlets.data.DimletType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * This class describes a dimension by its list of dimlets. It can be used to generate DimensionInformation objects
 *
 * A biome has:
 *  - Carvers: "cave", "hell_cave", "canyon", "underwater_canyon", "underwater_cave"
 *  - Features (structure, lake, ore, vegetation, ...)
 *      - Using a placement
 *  - Surface builder
 *
 * Biome provider
 *
 * Chunk generator
 */
public class DimensionDescriptor {

    private final List<DimletKey> dimlets = new ArrayList<>();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Gson GSON_COMPACT = new GsonBuilder().disableHtmlEscaping().create();

    public static final DimensionDescriptor EMPTY = new DimensionDescriptor();

    public List<DimletKey> getDimlets() {
        return dimlets;
    }

    public void read(String json) {
        JsonElement root = JsonParser.parseString(json);
        JsonArray object = root.getAsJsonArray();

        read(object);
    }

    public void read(JsonArray object) {
        dimlets.clear();
        for (JsonElement element : object) {
            JsonObject dimletJson = element.getAsJsonObject();
            String type;
            if (dimletJson.has("type")) {
                type = dimletJson.getAsJsonPrimitive("type").getAsString();
            } else {
                type = dimletJson.getAsJsonPrimitive("t").getAsString();
            }
            DimletType dimletType = DimletType.byName(type);
            String name;
            if (dimletJson.has("name")) {
                name = dimletJson.get("name").getAsString();
            } else {
                name = dimletJson.get("n").getAsString();
            }
            DimletKey dimletDescriptor = new DimletKey(dimletType, name);
            dimlets.add(dimletDescriptor);
        }
    }

    public String write() {
        JsonArray root = new JsonArray();
        for (DimletKey dimletDescriptor : dimlets) {
            JsonObject dimletJson = new JsonObject();
            dimletJson.addProperty("type", dimletDescriptor.type().name());
            dimletJson.addProperty("name", dimletDescriptor.key());
            root.add(dimletJson);
        }
        return GSON.toJson(root);
    }

    // Write a more compact form of the dimension. This is stored with the dimension itself
    public String compact() {
        JsonArray root = new JsonArray();
        for (DimletKey dimletDescriptor : dimlets) {
            JsonObject dimletJson = new JsonObject();
            dimletJson.addProperty("t", dimletDescriptor.type().getShortName());
            dimletJson.addProperty("n", dimletDescriptor.key());
            root.add(dimletJson);
        }
        return GSON_COMPACT.toJson(root);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DimensionDescriptor that = (DimensionDescriptor) o;
        return Objects.equals(dimlets, that.dimlets);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dimlets);
    }

    public void dump(Player player) {
        for (DimletKey dimlet : dimlets) {
            player.displayClientMessage(
                    new TextComponent(dimlet.type().name() + ": ")
                            .withStyle(ChatFormatting.AQUA)
                            .append(new TextComponent(dimlet.key()).withStyle(ChatFormatting.WHITE))
                    , false);
        }
    }

    public void log(String header) {
        StringBuilder headerBuilder = new StringBuilder("--------------------------------------------------\n" + header);
        for (DimletKey dimlet : dimlets) {
            headerBuilder.append("\n    ").append(dimlet.type().name()).append(": ").append(dimlet.key());
        }
        header = headerBuilder.toString();
        header += "\n--------------------------------------------------";
        RFToolsDim.setup.getLogger().info(header);
    }

    /// Create a randomized descriptor based on this one (i.e. completing the things that are missing randomly)
    public DimensionDescriptor createRandomizedDescriptor(Random random) {
        DimensionDescriptor randomizedDescriptor = new DimensionDescriptor();

        List<DimletKey> dimlets = getDimlets();
        List<DimletKey> randomized = randomizedDescriptor.getDimlets();

        if (!has(dimlets, DimletType.TERRAIN)) {
            DimletKey terrainDimlet = DimletDictionary.get().getRandomDimlet(DimletType.TERRAIN, random);
            if (terrainDimlet != null) {
                addRandomDimlets(DimletType.BLOCK, randomized, random, Math.min(1, random.nextInt(5)-2));
                addAttributeDimlets(randomized, random);
                addFluidDimlets(randomized, random, 1);
                randomized.add(terrainDimlet);
            }
        }
        if (!has(dimlets, DimletType.STRUCTURE)) {
            if (random.nextFloat() < .05) {
                randomized.add(new DimletKey(DimletType.STRUCTURE, "none"));
            } if (random.nextFloat() < .6) {
                randomized.add(new DimletKey(DimletType.STRUCTURE, "default"));
            } else {
                addRandomDimlets(DimletType.STRUCTURE, randomized, random, random.nextInt(4) + 1);
            }
        }

        if (!has(dimlets, DimletType.SKY)) {
            addRandomDimlets(DimletType.SKY, randomized, random, Math.max(0, random.nextInt(9) - 5));
        }

        if (!has(dimlets, DimletType.FEATURE)) {
            int cnt = random.nextInt(4);
            for (int i = 0 ; i < cnt-1 ; i++) {
                DimletKey featureDimlet = DimletDictionary.get().getRandomDimlet(DimletType.FEATURE, random);
                if (featureDimlet != null) {
                    if (random.nextFloat() < .2f) {
                        addTagDimlets(randomized, random);
                    } else {
                        addBlockDimlets(randomized, random);
                    }
                    randomized.add(featureDimlet);
                }
            }
        }
        boolean addedBiomeCategories = false;
        if (!has(dimlets, DimletType.BIOME_CATEGORY) && !has(dimlets, DimletType.BIOME)) {
            int cnt = random.nextInt(4);
            for (int i = 0 ; i < cnt-2 ; i++) {
                DimletKey categoryDimlet = DimletDictionary.get().getRandomDimlet(DimletType.BIOME_CATEGORY, random);
                if (categoryDimlet != null) {
                    randomized.add(categoryDimlet);
                    addedBiomeCategories = true;
                }
            }
        }
        if (!has(dimlets, DimletType.BIOME_CONTROLLER)) {
            DimletKey controllerDimlet = DimletDictionary.get().getRandomDimlet(DimletType.BIOME_CONTROLLER, random);
            if (controllerDimlet != null) {
                if (!addedBiomeCategories) {
                    addBiomeDimlets(randomized, random);
                }
                randomized.add(controllerDimlet);
            }
        }
        if (!has(dimlets, DimletType.TIME)) {
            DimletKey timeDimlet = DimletDictionary.get().getRandomDimlet(DimletType.TIME, random);
            if (timeDimlet != null) {
                randomized.add(timeDimlet);
            }
        }

        return randomizedDescriptor;
    }

    private void addAttributeDimlets(List<DimletKey> randomized, Random random) {
        addRandomDimlets(DimletType.ATTRIBUTE, randomized, random, random.nextInt(3));
    }

    private void addBiomeDimlets(List<DimletKey> randomized, Random random) {
        addRandomDimlets(DimletType.BIOME, randomized, random, random.nextInt(8)+1);
    }

    private void addFluidDimlets(List<DimletKey> randomized, Random random, int max) {
        addRandomDimlets(DimletType.FLUID, randomized, random, random.nextInt(Math.min(3, max+1)));
    }

    private void addTagDimlets(List<DimletKey> randomized, Random random) {
        addRandomDimlets(DimletType.TAG, randomized, random, Math.max(1, random.nextInt(4)-2));
    }

    private void addBlockDimlets(List<DimletKey> randomized, Random random) {
        addRandomDimlets(DimletType.BLOCK, randomized, random, Math.max(1, random.nextInt(6)-2));
    }

    private void addRandomDimlets(DimletType type, List<DimletKey> randomized, Random random, int cnt) {
        for (int i = 0 ; i < cnt ; i++) {
            DimletKey dimlet = DimletDictionary.get().getRandomDimlet(type, random);
            if (dimlet != null) {
                randomized.add(dimlet);
            }
        }
    }

    private boolean has(List<DimletKey> dimlets, DimletType type) {
        return dimlets.stream().anyMatch(key -> key.type() == type);
    }
}
