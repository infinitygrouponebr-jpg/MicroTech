package Infinitygroup.microtech.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class TechSwordClientKeybinds {
    public static final KeyMapping OPEN_ABILITY_SELECTOR = new KeyMapping(
            "key.microtech.tech_sword_ability_selector",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "key.categories.microtech"
    );

    private TechSwordClientKeybinds() {
    }
}
