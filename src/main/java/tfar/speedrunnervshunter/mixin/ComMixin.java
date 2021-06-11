package tfar.speedrunnervshunter.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tfar.speedrunnervshunter.ModConfig;

@Mixin(targets = "net/minecraft/client/renderer/item/ItemProperties$2")
public class ComMixin {

    @Inject(method = "getSpawnPosition(Lnet/minecraft/client/multiplayer/ClientLevel;)Lnet/minecraft/core/BlockPos;",at = @At("HEAD"),cancellable = true)
    private void endCompass(ClientLevel clientLevel, CallbackInfoReturnable<@Nullable BlockPos> cir) {
        if (ModConfig.compasses_work_in_end) {
            cir.setReturnValue(clientLevel.getSharedSpawnPos());
        }
    }
}
