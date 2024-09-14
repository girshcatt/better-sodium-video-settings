package dev.pieman.mixins;

import dev.architectury.platform.Platform;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.GameOptionsScreen;
import net.minecraft.client.gui.screen.option.VideoOptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.DirectionalLayoutWidget;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import org.apache.commons.lang3.ArrayUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;

@Mixin(VideoOptionsScreen.class)
public abstract class MixinVideoOptionsScreen extends GameOptionsScreen {
    @Unique
    Constructor<?> SodiumVideoOptionsScreenClassCtor;
    @Unique
    Constructor<?> SodiumOptionsGUIClassCtor;
    @Unique
    Field SodiumOptionsGUIClassPagesField;
    @Unique
    Class<?> SodiumOptionsGUIClass;

    public MixinVideoOptionsScreen(Screen parent, GameOptions gameOptions, Text title) {
        super(parent, gameOptions, title);
    }

    @Override
    protected void initFooter() {
        DirectionalLayoutWidget directionalLayoutWidget = this.layout.addFooter(DirectionalLayoutWidget.horizontal().spacing(8));
        directionalLayoutWidget.add(new ButtonWidget.Builder(Text.translatable("text.bsvsb.sodiumvideosettings"), (button) -> {
            if (Platform.isModLoaded("reeses-sodium-options")) {
                flashyReesesOptionsScreen();
            } else {
                sodiumVideoOptionsScreen();
            }
        }).build());
        directionalLayoutWidget.add(ButtonWidget.builder(ScreenTexts.DONE, (button) -> {
            this.close();
        }).build());
    }

    @Inject(method = "getOptions", at = @At("RETURN"), cancellable = true)
    private static void removeChunkBuilderButton(GameOptions gameOptions, CallbackInfoReturnable<SimpleOption<?>[]> cir) {
        var value = cir.getReturnValue();
        value = ArrayUtils.removeElement(value, gameOptions.getChunkBuilderMode());
        cir.setReturnValue(value);
    }

    @Unique
    void flashyReesesOptionsScreen() {
        if (SodiumVideoOptionsScreenClassCtor == null) {
            try {
                SodiumVideoOptionsScreenClassCtor = Class.forName("me.flashyreese.mods.reeses_sodium_options.client.gui.SodiumVideoOptionsScreen").getConstructor(Screen.class, List.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            assert this.client != null;
            ensureSodiumOptionsGUI();
            var tmpScreen = SodiumOptionsGUIClassCtor.newInstance(this);
            var pages = SodiumOptionsGUIClassPagesField.get(tmpScreen);
            this.client.setScreen((Screen) SodiumVideoOptionsScreenClassCtor.newInstance(this, pages));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Unique
    void ensureSodiumOptionsGUI()
    {
        if (SodiumOptionsGUIClass == null) {
            try {
                if (Integer.parseInt(Platform.getMod("sodium").getVersion().split("[.]")[1]) >= 6) {
                    SodiumOptionsGUIClass = Class.forName("net.caffeinemc.mods.sodium.client.gui.SodiumOptionsGUI");
                } else {
                    SodiumOptionsGUIClass = Class.forName("me.jellysquid.mods.sodium.client.gui.SodiumOptionsGUI");
                }
                //Use declaredConstructors to get the correct constructor, will break when adding more constructors
                //Also consider using the public static method (public static Screen createScreen(Screen currentScreen)) (https://github.com/CaffeineMC/sodium-fabric/blob/db7c77a0960c166e4177acbbabd892167fcd0271/src/main/java/net/caffeinemc/mods/sodium/client/gui/SodiumOptionsGUI.java#L124C5-L124C62)
                //This will most likely also need a rework in the future, because the sodium gui is getting reworked (https://github.com/CaffeineMC/sodium-fabric/issues/2562#issuecomment-2180808114)
                SodiumOptionsGUIClassCtor = SodiumOptionsGUIClass.getDeclaredConstructors()[0];
                //New constructor is private, so make it accessible
                SodiumOptionsGUIClassCtor.setAccessible(true);
                SodiumOptionsGUIClassPagesField = SodiumOptionsGUIClass.getDeclaredField("pages");
                SodiumOptionsGUIClassPagesField.setAccessible(true);
            } catch (Exception e) {
                System.out.println("crash ensure");
                e.printStackTrace();
            }
        }
    }

    @Unique
    void sodiumVideoOptionsScreen() {
        ensureSodiumOptionsGUI();
        try {
            assert this.client != null;
            this.client.setScreen((Screen) SodiumOptionsGUIClassCtor.newInstance(this));
        } catch (Exception e) {
            System.out.println("crash on sodiumVideoOptions");
            e.printStackTrace();
        }
    }
}
