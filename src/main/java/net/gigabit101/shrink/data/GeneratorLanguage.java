package net.gigabit101.shrink.data;

import net.gigabit101.shrink.Shrink;
import net.gigabit101.shrink.items.ShrinkItems;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.common.data.LanguageProvider;

public class GeneratorLanguage extends LanguageProvider
{
    public GeneratorLanguage(DataGenerator gen)
    {
        super(gen, Shrink.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations()
    {
        add("itemGroup.shrink", "Shrink");
        add("key.shrink.category", "Shrink");
        add("key.shrink.shrink", "Toggle Shrink");
        addItem(ShrinkItems.SHRINKING_DEVICE, "Personal Shrinking Device");
        addItem(ShrinkItems.MOB_BOTTLE, "Glass Bottle");
//        addItem(ShrinkItems.SHRINK_RAY, "Shrink Ray");
    }

    private void addPrefixed(String key, String text)
    {
        add(String.format("%s.%s", Shrink.MOD_ID, key), text);
    }
}
