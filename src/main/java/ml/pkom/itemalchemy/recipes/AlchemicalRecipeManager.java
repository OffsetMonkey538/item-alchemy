package ml.pkom.itemalchemy.recipes;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import ml.pkom.itemalchemy.ItemAlchemy;
import net.minecraft.datafixer.fix.BlockEntitySignTextStrictJsonFix;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.ShapelessRecipe;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.registry.Registry;
import org.apache.commons.io.IOUtils;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;

// Referred to "TransmutationRecipeManager (RetroExchange)"
// https://github.com/modmuss50/Retro-Exchange/blob/main/common/src/main/java/me/modmuss50/retroexchange/TransmutationRecipeManager.java (modmuss50)
public class AlchemicalRecipeManager {

    private int count = 0;
    private final Map<RecipeType<?>, ImmutableMap.Builder<Identifier, Recipe<?>>> map;

    public AlchemicalRecipeManager(Map<RecipeType<?>, ImmutableMap.Builder<Identifier, Recipe<?>>> map) {
        this.map = map;
    }

    public void apply(ResourceManager resourceManager) {
        count = 0;

        Collection<Identifier> resourceIds = resourceManager.findResources("alchemical_craft", s -> s.endsWith(".json"));

        resourceIds.forEach(resourceId -> {
            try {
                Resource resource = resourceManager.getResource(resourceId);

                String json = IOUtils.toString(resource.getInputStream(), StandardCharsets.UTF_8);
                resource.close();
                JsonArray jsonArray = BlockEntitySignTextStrictJsonFix.GSON.fromJson(json, JsonArray.class);
                jsonArray.forEach((jsonElement) -> {
                    handle(jsonElement.getAsJsonObject());
                });
            } catch (Exception e) {
                e.printStackTrace();
                ItemAlchemy.LOGGER.error("Failed to read {}", resourceId.toString(), e);
            }
        });

        addAllSmelting();
    }

    private void handle(JsonObject jsonObject) {
        JsonArray itemsArray = jsonObject.get("items").getAsJsonArray();

        Identifier inputId = new Identifier(itemsArray.get(0).getAsString());
        Identifier outputId = new Identifier(itemsArray.get(1).getAsString());

        Item input = Registry.ITEM.get(inputId);
        Item output = Registry.ITEM.get(outputId);

        int amount = jsonObject.get("amount").getAsInt();

        addAlchemicalRecipe(input, output, amount);
    }

    public void addAlchemicalRecipe(Item item, Object input, int size) {
        Object[] inputs = new Object[size + 1];
        for (int i = 1; i < size + 1; i++) {
            inputs[i] = input;
        }
        inputs[0] = ml.pkom.itemalchemy.Items.PHILOSOPHER_STONE.get();
        addShapelessRecipe(new ItemStack(item), inputs);
    }

    public void addAllSmelting() {
        map.get(RecipeType.SMELTING).build()
                .values().stream()
                .filter(recipe -> recipe.getType() == RecipeType.SMELTING)
                .forEach(recipe -> {
                    ItemStack output = recipe.getOutput();

                    for (Ingredient input : recipe.getIngredients()) {
                        if (output.isEmpty() || input.isEmpty()) {
                            return;
                        }

                        ItemStack stack = output.copy();
                        stack.setCount(7);
                        addShapelessRecipe(stack, ml.pkom.itemalchemy.Items.PHILOSOPHER_STONE.get(), Items.COAL, input, input, input, input, input, input, input);
                    }
                });
    }

    private Identifier getNextID() {
        return ItemAlchemy.id("alchemical_craft/id_" + count++);
    }

    public void addShapelessRecipe(ItemStack stack, Object... input) {
        ShapelessRecipe recipe = new ShapelessRecipe(getNextID(), "", stack, buildInput(input));
        map.get(recipe.getType()).put(recipe.getId(), recipe);
    }

    private DefaultedList<Ingredient> buildInput(Object[] input) {
        DefaultedList<Ingredient> list = DefaultedList.of();
        for (Object obj : input) {
            Ingredient ingredient = null;
            if (obj instanceof Ingredient)
                ingredient = (Ingredient) obj;

            if (obj instanceof ItemConvertible)
                ingredient = Ingredient.ofItems((ItemConvertible) obj);

            list.add(ingredient);
        }
        return list;
    }
}